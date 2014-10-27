package edu.thu.ss.lang.analyzer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.ss.lang.pojo.DataAssociation;
import edu.thu.ss.lang.pojo.DataCategory;
import edu.thu.ss.lang.pojo.DataCategoryContainer;
import edu.thu.ss.lang.pojo.DataCategoryRef;
import edu.thu.ss.lang.pojo.Desensitization;
import edu.thu.ss.lang.pojo.HierarchicalObject;
import edu.thu.ss.lang.pojo.ObjectRef;
import edu.thu.ss.lang.pojo.Restriction;
import edu.thu.ss.lang.pojo.Rule;
import edu.thu.ss.lang.pojo.UserCategory;
import edu.thu.ss.lang.pojo.UserCategoryContainer;
import edu.thu.ss.lang.pojo.UserCategoryRef;

/**
 * Resolve reference elements into concrete elements.
 * Also materialize user and data references.
 * 
 * @author luochen
 * 
 */
public class RuleResolver extends BaseRuleAnalyzer {

	private static Logger logger = LoggerFactory.getLogger(RuleResolver.class);

	@Override
	public boolean analyzeRule(Rule rule, UserCategoryContainer users, DataCategoryContainer datas) {
		boolean error = false;
		error = error || resolveUsers(rule.getUserRefs(), users);
		error = error || resolveDatas(rule.getDataRefs(), datas);
		for (DataAssociation association : rule.getAssociations()) {
			error = error || resolveDatas(association.getDataRefs(), datas);
		}
		for (Restriction restriction : rule.getRestrictions()) {
			if (restriction.isForbid()) {
				continue;
			}
			for (Desensitization de : restriction.getDesensitizations()) {
				error = error || resolveDesensitization(de, rule);
			}
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

	private boolean resolveUsers(Set<UserCategoryRef> refs, UserCategoryContainer users) {
		boolean error = false;
		UserCategory user = null;
		for (UserCategoryRef ref : refs) {
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

			ref.materialize();
		}
		return error;
	}

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

	private UserCategory resolveUser(ObjectRef ref, UserCategoryContainer container, String ruleId) {
		UserCategory user = container.get(ref.getRefid());
		if (user == null) {
			logger.error("Fail to location user category: " + ref.getRefid() + ", referenced in rule: " + ruleId);
		}
		return user;
	}

	private boolean resolveDatas(Set<DataCategoryRef> refs, DataCategoryContainer datas) {
		boolean error = false;
		for (DataCategoryRef ref : refs) {
			DataCategory data = resolveData(ref, datas);
			if (data != null) {
				ref.setData(data);
			} else {
				error = true;
			}
			for (ObjectRef excludeRef : ref.getExcludeRefs()) {
				data = resolveData(excludeRef, datas);
				if (data != null && !checkExclusion(ref.getData(), data)) {
					ref.getExcludes().add(data);
				} else {
					error = true;
				}
			}
			ref.materialize();
		}
		return error;
	}

	private DataCategory resolveData(ObjectRef ref, DataCategoryContainer container) {
		DataCategory data = container.get(ref.getRefid());
		if (data == null) {
			logger.error("Fail to location data category: " + ref.getRefid() + ", referenced in rule: " + ruleId);
		}
		return data;
	}

	private boolean resolveDesensitization(Desensitization de, Rule rule) {
		if (rule.getAssociations().size() > 0 && rule.getDataRefs().size() > 0) {
			logger.error(
					"Data-category-ref and data-association should not appear together when restriction contains desensitize element in rule: {}",
					ruleId);
			return true;
		}
		if (rule.getDataRefs().size() > 0) {
			if (de.getObjRefs().size() > 0) {
				logger.error("No data-category-ref element should appear in desensitize element when only data category is referenced in rule: "
						+ ruleId);
				return true;
			}
			return false;
		} else if (rule.getAssociations().size() > 0) {
			if (de.getObjRefs().size() == 0) {
				logger.error("Restricted data category must be specified explicitly when data association is referenced by rule: "
						+ ruleId);
				return true;
			}
		}
		boolean error = false;
		for (ObjectRef ref : de.getObjRefs()) {
			DataCategoryRef data = null;
			for (DataAssociation association : rule.getAssociations()) {
				data = association.get(ref.getRefid());
				if (data == null) {
					logger.error("Restricted data category: {} must be contained in all data associations in rule: {}",
							ref.getRefid(), ruleId);
					error = true;
				}
			}
			if (!error) {
				de.getDataRefs().add(data);
			}
		}
		return error;
	}
}