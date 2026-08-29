insert into fees (fee_code, fee_name, fee_type, updated_on, updated_by)
values
    ('ANNUAL', 'Annual Fee', 'FLAT', current_timestamp, 'seed'),
    ('ATM', 'ATM Fee', 'FLAT', current_timestamp, 'seed'),
    ('EARLYWDRAW', 'Early Withdrawal Fee', 'PERCENT', current_timestamp, 'seed'),
    ('EXTOVERDRAFT', 'Extended Overdraft Fee', 'FLAT', current_timestamp, 'seed'),
    ('FOREIGNATM', 'Foreign ATM Fee', 'FLAT', current_timestamp, 'seed'),
    ('FOREIGNTXN', 'Foreign Transaction Fee', 'PERCENT', current_timestamp, 'seed'),
    ('INACTIVITY', 'Inactivity Fee', 'FLAT', current_timestamp, 'seed'),
    ('LATEPMT', 'Late Payment Fee', 'FLAT', current_timestamp, 'seed'),
    ('MONTHLY', 'Monthly Fee', 'FLAT', current_timestamp, 'seed'),
    ('ORIGIN', 'Origination Fee', 'PERCENT', current_timestamp, 'seed'),
    ('OVERDRAFT', 'Overdraft Fee', 'FLAT', current_timestamp, 'seed'),
    ('PROCESS', 'Processing Fee', 'FLAT', current_timestamp, 'seed'),
    ('RETPAYMENT', 'Returned Payment Fee', 'FLAT', current_timestamp, 'seed'),
    ('WIRE', 'Wire Fee', 'FLAT', current_timestamp, 'seed');

insert into fee_product_types (fee_id, product_type)
select fee.id, product_types.product_type
from fees fee
join (values
    ('ANNUAL', 'DEPOSIT'), ('ANNUAL', 'CREDIT'),
    ('ATM', 'DEPOSIT'), ('EARLYWDRAW', 'CD'),
    ('EXTOVERDRAFT', 'DEPOSIT'), ('FOREIGNATM', 'DEPOSIT'),
    ('FOREIGNTXN', 'DEPOSIT'), ('INACTIVITY', 'DEPOSIT'),
    ('LATEPMT', 'CREDIT'), ('MONTHLY', 'DEPOSIT'),
    ('ORIGIN', 'CREDIT'), ('OVERDRAFT', 'DEPOSIT'),
    ('PROCESS', 'CREDIT'), ('RETPAYMENT', 'DEPOSIT'),
    ('RETPAYMENT', 'CREDIT'), ('WIRE', 'DEPOSIT')
) as product_types(fee_code, product_type) on product_types.fee_code = fee.fee_code;
