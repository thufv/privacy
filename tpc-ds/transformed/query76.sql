SELECT channel
	,col_name
	,d_year
	,d_qoy
	,i_category
	,COUNT(*) sales_cnt
	,SUM(ext_sales_price) sales_amt
FROM (
	SELECT 'store' AS channel
		,'ss_hdemo_sk' col_name
		,d_year
		,d_qoy
		,i_category
		,ss_ext_sales_price ext_sales_price
	FROM store_sales
	JOIN item
	JOIN date_dim
	WHERE ss_hdemo_sk IS NULL
		AND ss_sold_date_sk = d_date_sk
		AND ss_item_sk = i_item_sk
	
	UNION ALL
	
	SELECT 'web' AS channel
		,'ws_promo_sk' col_name
		,d_year
		,d_qoy
		,i_category
		,ws_ext_sales_price ext_sales_price
	FROM web_sales
	JOIN item
	JOIN date_dim
	WHERE ws_promo_sk IS NULL
		AND ws_sold_date_sk = d_date_sk
		AND ws_item_sk = i_item_sk
	
	UNION ALL
	
	SELECT 'catalog' AS channel
		,'cs_bill_customer_sk' col_name
		,d_year
		,d_qoy
		,i_category
		,cs_ext_sales_price ext_sales_price
	FROM catalog_sales
	JOIN item
	JOIN date_dim
	WHERE cs_bill_customer_sk IS NULL
		AND cs_sold_date_sk = d_date_sk
		AND cs_item_sk = i_item_sk
	) foo
GROUP BY channel
	,col_name
	,d_year
	,d_qoy
	,i_category
ORDER BY channel
	,col_name
	,d_year
	,d_qoy
	,i_category;