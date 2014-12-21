SELECT ca_zip
	,ca_state
	,SUM(ws_sales_price)
FROM web_sales
JOIN customer
JOIN customer_address
JOIN date_dim
JOIN item ON i_item_id = item.i_item_id
WHERE ws_bill_customer_sk = c_customer_sk
	AND c_current_addr_sk = ca_address_sk
	AND ws_item_sk = i_item_sk
	AND (
		SUBSTR(ca_zip, 1, 5) IN (
			'85669'
			,'86197'
			,'88274'
			,'83405'
			,'86475'
			,'85392'
			,'85460'
			,'80348'
			,'81792'
			)
		)
	AND ws_sold_date_sk = d_date_sk
	AND d_qoy = 2
	AND d_year = 1999
GROUP BY ca_zip
	,ca_state
ORDER BY ca_zip
	,ca_state;
