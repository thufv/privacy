package edu.thu.ss.spec.lang.analyzer.rule;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ss.spec.lang.pojo.DataAssociation;
import edu.thu.ss.spec.lang.pojo.DataCategory;
import edu.thu.ss.spec.lang.pojo.DataContainer;
import edu.thu.ss.spec.lang.pojo.DataRef;
import edu.thu.ss.spec.lang.pojo.Desensitization;
import edu.thu.ss.spec.lang.pojo.DesensitizeOperation;
import edu.thu.ss.spec.lang.pojo.HierarchicalObject;
import edu.thu.ss.spec.lang.pojo.ObjectRef;
import edu.thu.ss.spec.lang.pojo.Restriction;
import edu.thu.ss.spec.lang.pojo.Rule;
import edu.thu.ss.spec.lang.pojo.UserCategory;
import edu.thu.ss.spec.lang.pojo.UserContainer;
import edu.thu.ss.spec.lang.pojo.UserRef;

/**
 * Resolve reference elements into concrete elements in rules.
 * Also materialize {@link DataRef} and {@link UserRef}
 * 
 * @author luochen
 * 
 */
public class RuleResolver extends BaseRuleAnalyzer {

	private static Logger logger = LoggerFactory.getLogger(RuleResolver.class);

	/**
	 * caches descendants for {@link UserCategory}
	 */
	private Map<UserCategory, Set<UserCategory>> userCache = new HashMap<>();

	/**
	 * caches descendants for {@link DataCategory}
	 */
	private Map<DataCategory, Set<DataCategory>> dataCache = new HashMap<>();

	@Override
	public boolean analyzeRule(Rule rule, UserContainer users, DataContainer datas) {
		boolean error = false;
		if (rule.getAssociation() != null && rule.getDataRefs().size() > 0) {
			logger
					.error(
							"Data-category-ref and data-association should not appear together when restriction contains desensitize element in rule: {}",
							ruleId);
			return true;
		}

		error = error || resolveUsers(rule.getUserRefs(), users);
		error = error || resolveDatas(rule.getDataRefs(), datas);

		if (rule.getAssociation() != null) {
			error = error || resolveDatas(rule.getAssociation().getDataRefs(), datas);
		}

		for (Restriction restriction : rule.getRestrictions()) {
			if (restriction.isForbid()) {
				continue;
			}
			error = error || resolveRestriction(restriction, rule);

		}
		return error;
	}

	@Override
	public boolean stopOnError() {
		return true;
	}

	@Override
	public String errorMsg() {
		return "Error detected when resolving category references in rules, see error messages above.";
	}

	private boolean resolveUsers(List<UserRef> refs, UserContainer users) {
		boolean error = false;
		UserCategory user = null;
		for (UserRef ref : refs) {
			user = resolveUser(ref, users, ruleId);
			if (user != null) {
				ref.setUser(user);
			} else {
				error = true;
			}
			for (ObjectRef excludeRef : ref.getExcludeRefs()) {
				user = resolveUser(excludeRef, users, ruleId);
				if (user != null && !checkExclusion(ref.getUser(), user)) {
					ref.getExcludes().add(user);
				} else {
					error = true;
				}
			}

			ref.materialize(policy.getUserContainer(), userCache);
		}
		return error;
	}

	/**
	 * checks whether excluded category is descendant of referred category 
	 * @param category
	 * @param exclude
	 * @return error
	 */
	@SuppressWarnings("unchecked")
	private <T extends HierarchicalObject<T>> boolean checkExclusion(HierarchicalObject<T> category,
			HierarchicalObject<T> exclude) {
		if (category == null) {
			return true;
		}
		if (!category.ancestorOf((T) exclude) || category.equals(exclude)) {
			logger.error("Excluded category: {} must be a sub-category of referenced category: {}", exclude.getId(),
					category.getId());
			return true;
		}
		return false;
	}

	private boolean resolveDatas(Collection<DataRef> refs, DataContainer datas) {
		boolean error = false;
		for (DataRef ref : refs) {
			DataCategory data = resolveData(ref, datas);
			if (data != null) {
				ref.setData(data);
			} else {
				error = true;
			}
			for (ObjectRef excludeRef : ref.getExcludeRefs()) {
				data = resolveData(excludeRef, datas);
				if (data != null && !checkExclusion(ref.getData(), data)) {
					ref.exclude(data);
				} else {
					error = true;
				}
			}
			if (!error) {
				ref.materialize(policy.getDataContainer(), dataCache);
			}
		}
		return error;
	}

	private UserCategory resolveUser(ObjectRef ref, UserContainer container, String ruleId) {
		UserCategory user = container.get(ref.getRefid());
		if (user == null) {
			logger.error("Fail to locate user category: " + ref.getRefid() + ", referenced in rule: " + ruleId);
		}
		return user;
	}

	private DataCategory resolveData(ObjectRef ref, DataContainer container) {
		DataCategory data = container.get(ref.getRefid());
		if (data == null) {
			logger.error("Fail to locate data category: " + ref.getRefid() + ", referenced in rule: " + ruleId);
		}
		return data;
	}

	/**
	 * resolve {@link DesensitizeOperation} in {@link Desensitization}
	 * also performs constraint checks.
	 * @param de
	 * @param rule
	 * @return error
	 */
	private boolean resolveRestriction(Restriction res, Rule rule) {
		if (rule.getAssociation() == null) {
			return resolveSingleRestriction(res, rule);
		} else {
			return resolveAssociateRestriction(res, rule);
		}
	}

	private boolean resolveSingleRestriction(Restriction res, Rule rule) {
		if (res.getList().size() > 1) {
			logger.error("Only 1 desensitization is allowed for single rule: {}", rule.getId());
			return true;
		}
		Desensitization de = res.getList().get(0);
		if (de.getDataRefId() != null) {
			logger
					.error("No data-category-ref element should appear in desensitize element when only data category is referenced in rule: "
							+ ruleId);
			return true;
		}
		Desensitization[] des = new Desensitization[] { de };
		res.setDesensitizations(des);
		return false;
	}

	private boolean resolveAssociateRestriction(Restriction res, Rule rule) {
		Desensitization[] des = new Desensitization[rule.getAssociation().getDimension()];
		DataAssociation association = rule.getAssociation();
		boolean error = false;
		for (Desensitization de : res.getList()) {
			if (de.getDataRefId() == null) {
				logger
						.error("Restricted data category must be specified explicitly when data association is referenced by rule: "
								+ ruleId);
				return true;
			}
			String refid = de.getDataRefId();
			DataRef ref = association.get(refid);
			if (ref == null) {
				logger.error("Restricted data category: {} must be contained in referenced data association in rule: {}",
						refid, ruleId);
				error = true;
				continue;
			}
			de.setDataRef(ref);
			de.materialize();
			int index = association.getIndex(refid);
			des[index] = de;
		}

		res.setDesensitizations(des);

		return error;
	}

}
