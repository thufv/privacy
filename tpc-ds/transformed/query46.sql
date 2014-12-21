SELECT c_last_name
	,c_first_name
	,ca_city
	,bought_city
	,ss_ticket_number
	,amt
	,profit
FROM (
	SELECT ss_ticket_number
		,ss_customer_sk
		,ca_city bought_city
		,SUM(ss_coupon_amt) amt
		,SUM(ss_net_profit) profit
	FROM store_sales
	JOIN date_dim
	JOIN store
	JOIN household_demographics
	JOIN customer_address
	WHERE store_sales.ss_sold_date_sk = date_dim.d_date_sk
		AND store_sales.ss_store_sk = store.s_store_sk
		AND store_sales.ss_hdemo_sk = household_demographics.hd_demo_sk
		AND store_sales.ss_addr_sk = customer_address.ca_address_sk
		AND (
			household_demographics.hd_dep_count = 8
			OR household_demographics.hd_vehicle_count = - 1
			)
		AND date_dim.d_dow IN (
			6
			,0
			)
		AND date_dim.d_year IN (
			2000
			,2000 + 1
			,2000 + 2
			)
		AND store.s_city IN (
			'Midway'
			,'Fairview'
			,'Fairview'
			,'Fairview'
			,'Fairview'
			)
	GROUP BY ss_ticket_number
		,ss_customer_sk
		,ss_addr_sk
		,ca_city
	) dn
JOIN customer
JOIN customer_address current_addr
WHERE ss_customer_sk = c_customer_sk
	AND customer.c_current_addr_sk = current_addr.ca_address_sk
	AND current_addr.ca_city <> bought_city
ORDER BY c_last_name
	,c_first_name
	,ca_city
	,bought_city
	,ss_ticket_number;
