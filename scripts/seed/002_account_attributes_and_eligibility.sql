insert into account_attributes (attribute_code, attribute_name, attribute_type, updated_on, updated_by)
values
    ('AGE', 'Account Holder Age', 'INTEGER', current_timestamp, 'seed'),
    ('BAL', 'Account Balance', 'DECIMAL', current_timestamp, 'seed'),
    ('STATE', 'Customer State', 'TEXT', current_timestamp, 'seed'),
    ('TENURE', 'Customer Tenure', 'INTEGER', current_timestamp, 'seed'),
    ('INCOME', 'Annual Income', 'DECIMAL', current_timestamp, 'seed'),
    ('LTV', 'Loan To Value Ratio', 'DECIMAL', current_timestamp, 'seed'),
    ('TERM', 'Loan Term Months', 'INTEGER', current_timestamp, 'seed'),
    ('OPENED', 'Account Opened Date', 'DATE', current_timestamp, 'seed'),
    ('AUTOPAY', 'Auto Pay Enrolled', 'BOOLEAN', current_timestamp, 'seed');

insert into account_attribute_product_types (attribute_id, product_type)
select attribute.id, product_types.product_type
from account_attributes attribute
join (values
    ('AGE', 'DEPOSIT'), ('AGE', 'CD'), ('AGE', 'CREDIT'),
    ('BAL', 'DEPOSIT'), ('BAL', 'CD'), ('STATE', 'CREDIT'),
    ('TENURE', 'DEPOSIT'), ('INCOME', 'CREDIT'), ('LTV', 'CREDIT'),
    ('TERM', 'CREDIT'), ('OPENED', 'DEPOSIT'), ('OPENED', 'CD'),
    ('AUTOPAY', 'CREDIT')
) as product_types(attribute_code, product_type)
    on product_types.attribute_code = attribute.attribute_code;

insert into eligibility_reasons (reason_code, reason_name, updated_on, updated_by)
values
    ('LOYAL', 'Loyal Customer', current_timestamp, 'seed'),
    ('PREMIUM', 'Premium Balance', current_timestamp, 'seed'),
    ('INCOME', 'High Income Borrower', current_timestamp, 'seed'),
    ('LOWLTV', 'Low Loan To Value', current_timestamp, 'seed'),
    ('YOUNG', 'Young Customer', current_timestamp, 'seed'),
    ('ESTABLISHED', 'Established Account', current_timestamp, 'seed'),
    ('SENIOR', 'Senior Customer', current_timestamp, 'seed'),
    ('AUTOPAY', 'Auto Pay Enrolled', current_timestamp, 'seed'),
    ('SAVAGE', 'Mature Saver', current_timestamp, 'seed'),
    ('SAVBAL', 'Savings Balance', current_timestamp, 'seed'),
    ('SAVTENURE', 'Long-Term Saver', current_timestamp, 'seed'),
    ('CHKPAGE', 'Mature Premium Customer', current_timestamp, 'seed'),
    ('CHKPBAL', 'Premium Checking Balance', current_timestamp, 'seed'),
    ('CHKPTENURE', 'Long-Term Premium Customer', current_timestamp, 'seed');

insert into eligibility_reason_conditions (reason_id, attribute_id, operator, attribute_value)
select reason.id, attribute.id, conditions.operator, conditions.attribute_value
from eligibility_reasons reason
join (values
    ('LOYAL', 'TENURE', '>=', '5'),
    ('PREMIUM', 'BAL', '>=', '50000'),
    ('INCOME', 'INCOME', '>=', '75000'), ('LOWLTV', 'LTV', '<=', '80'),
    ('YOUNG', 'AGE', '<', '40'), ('ESTABLISHED', 'OPENED', '<=', '2020-01-01'),
    ('SENIOR', 'AGE', '>=', '60'), ('AUTOPAY', 'AUTOPAY', '=', 'true'),
    ('SAVAGE', 'AGE', '>=', '40'), ('SAVBAL', 'BAL', '>=', '20000'),
    ('SAVTENURE', 'TENURE', '>=', '7'), ('CHKPAGE', 'AGE', '>=', '50'),
    ('CHKPBAL', 'BAL', '>=', '45000'), ('CHKPTENURE', 'TENURE', '>=', '9')
) as conditions(reason_code, attribute_code, operator, attribute_value)
    on conditions.reason_code = reason.reason_code
join account_attributes attribute on attribute.attribute_code = conditions.attribute_code;
