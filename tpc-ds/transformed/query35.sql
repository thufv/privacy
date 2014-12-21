SELECT ca_state
	,cd_gender
	,cd_marital_status
	,COUNT(*) cnt1
	,stddev_samp(cd_dep_count)
	,SUM(cd_dep_count)
	,MIN(cd_dep_count)
	,cd_dep_employed_count
	,COUNT(*) cnt2
	,stddev_samp(cd_dep_employed_count)
	,SUM(cd_dep_employed_count)
	,MIN(cd_dep_employed_count)
	,cd_dep_college_count
	,COUNT(*) cnt3
	,stddev_samp(cd_dep_college_count)
	,SUM(cd_dep_college_count)
	,MIN(cd_dep_college_count)
FROM customer c
JOIN customer_address ca
JOIN customer_demographics
JOIN catalog_sales
JOIN web_sales
JOIN store_sales
JOIN date_dim ON (c.c_customer_sk = ss_customer_sk
	AND (
		(
			c.c_customer_sk = ws_bill_customer_sk
			AND ws_sold_date_sk = d_date_sk
			)
		OR (
			c.c_customer_sk = cs_ship_customer_sk
			AND cs_sold_date_sk = d_date_sk
			)
		)
	AND ss_sold_date_sk = d_date_sk
	AND d_year = 2001
	AND d_qoy < 4)
WHERE c.c_current_addr_sk = ca.ca_address_sk
	AND cd_demo_sk = c.c_current_cdemo_sk
GROUP BY ca_state
	,cd_gender
	,cd_marital_status
	,cd_dep_count
	,cd_dep_employed_count
	,cd_dep_college_count
ORDER BY ca_state
	,cd_gender
	,cd_marital_status
	,cd_dep_count
	,cd_dep_employed_count
	,cd_dep_college_count;
