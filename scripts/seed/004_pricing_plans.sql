insert into pricing_plans (
    plan_code, plan_name, product_id, region_id, active_from, active_through, updated_on, updated_by
)
select plans.plan_code, plans.plan_name, product.id, region.id,
       plans.active_from, plans.active_through, current_timestamp, 'seed'
from (values
    ('CHK27H1', '2027 Checking First Half', 'CHK', 'EAST', date '2027-01-01', date '2027-06-30'),
    ('CHK27H2', '2027 Checking Second Half', 'CHK', 'EAST', date '2027-07-01', date '2027-12-31'),
    ('CHKP27H1', '2027 Premium Checking First Half', 'CHKPLUS', 'MID', date '2027-01-01', date '2027-06-30'),
    ('CHKP27H2', '2027 Premium Checking Second Half', 'CHKPLUS', 'MID', date '2027-07-01', date '2027-12-31'),
    ('SAV27H1', '2027 Savings First Half', 'SAV', 'MID', date '2027-01-01', date '2027-06-30'),
    ('SAV27H2', '2027 Savings Second Half', 'SAV', 'MID', date '2027-07-01', date '2027-12-31'),
    ('MMS27H1', '2027 Money Market First Half', 'MMSAV', 'EAST', date '2027-01-01', date '2027-06-30'),
    ('MMS27H2', '2027 Money Market Second Half', 'MMSAV', 'EAST', date '2027-07-01', date '2027-12-31'),
    ('CD27H1', '2027 Certificate First Half', 'CD12', 'SOUTH', date '2027-01-01', date '2027-06-30'),
    ('CD27H2', '2027 Certificate Second Half', 'CD12', 'SOUTH', date '2027-07-01', date '2027-12-31'),
    ('CD327H1', '2027 3 Month Certificate First Half', 'CD3', 'MID', date '2027-01-01', date '2027-06-30'),
    ('CD327H2', '2027 3 Month Certificate Second Half', 'CD3', 'MID', date '2027-07-01', date '2027-12-31'),
    ('AUTO27H1', '2027 Auto Loan First Half', 'AUTO', 'EAST', date '2027-01-01', date '2027-06-30'),
    ('AUTO27H2', '2027 Auto Loan Second Half', 'AUTO', 'EAST', date '2027-07-01', date '2027-12-31'),
    ('PERS27H1', '2027 Personal Loan First Half', 'PERS', 'MID', date '2027-01-01', date '2027-06-30'),
    ('PERS27H2', '2027 Personal Loan Second Half', 'PERS', 'MID', date '2027-07-01', date '2027-12-31'),
    ('MORT27H1', '2027 Home Mortgage First Half', 'MORT', 'SOUTH', date '2027-01-01', date '2027-06-30'),
    ('MORT27H2', '2027 Home Mortgage Second Half', 'MORT', 'SOUTH', date '2027-07-01', date '2027-12-31')
) as plans(plan_code, plan_name, product_code, region_code, active_from, active_through)
join products product on product.product_code = plans.product_code
join regions region on region.region_code = plans.region_code;

insert into pricing_plan_fees (pricing_plan_id, fee_id, amount)
select plan.id, fee.id,
       case
           when product.product_code in ('CHK', 'CHKPLUS') and fee.fee_code = 'MONTHLY' then 12.00
           when product.product_code = 'MORT' and fee.fee_code = 'PROCESS' then 500.00
           else prices.amount
       end
from pricing_plans plan
join products product on product.id = plan.product_id
join fee_product_types fee_product_type on fee_product_type.product_type = product.product_type
join fees fee on fee.id = fee_product_type.fee_id
join (values
    ('DEPOSIT', 'ANNUAL', 25.00), ('DEPOSIT', 'ATM', 3.00),
    ('DEPOSIT', 'EXTOVERDRAFT', 35.00), ('DEPOSIT', 'FOREIGNATM', 5.00),
    ('DEPOSIT', 'FOREIGNTXN', 3.00), ('DEPOSIT', 'INACTIVITY', 10.00),
    ('DEPOSIT', 'MONTHLY', 5.00), ('DEPOSIT', 'OVERDRAFT', 35.00),
    ('DEPOSIT', 'RETPAYMENT', 25.00), ('DEPOSIT', 'WIRE', 20.00),
    ('CD', 'EARLYWDRAW', 2.50),
    ('CREDIT', 'ANNUAL', 50.00), ('CREDIT', 'LATEPMT', 35.00),
    ('CREDIT', 'ORIGIN', 1.00), ('CREDIT', 'PROCESS', 150.00),
    ('CREDIT', 'RETPAYMENT', 25.00)
) as prices(product_type, fee_code, amount)
    on prices.product_type = product.product_type
   and prices.fee_code = fee.fee_code;

insert into pricing_plan_fee_reasons (pricing_plan_id, fee_id, reason_id)
select plan.id, fee.id, reason.id
from (values
    ('CHK27H1', 'MONTHLY', 'LOYAL'), ('CHK27H2', 'MONTHLY', 'LOYAL'),
    ('CHK27H1', 'MONTHLY', 'YOUNG'), ('CHK27H2', 'MONTHLY', 'YOUNG'),
    ('CHK27H1', 'ANNUAL', 'YOUNG'), ('CHK27H2', 'ANNUAL', 'YOUNG'),
    ('CHK27H1', 'ATM', 'YOUNG'), ('CHK27H2', 'ATM', 'YOUNG'),
    ('CHK27H1', 'OVERDRAFT', 'YOUNG'), ('CHK27H2', 'OVERDRAFT', 'YOUNG'),
    ('CHKP27H1', 'MONTHLY', 'LOYAL'), ('CHKP27H2', 'MONTHLY', 'LOYAL'),
    ('CHKP27H1', 'MONTHLY', 'ESTABLISHED'),
    ('CHKP27H2', 'MONTHLY', 'ESTABLISHED'),
    ('CHKP27H1', 'ANNUAL', 'CHKPAGE'), ('CHKP27H2', 'ANNUAL', 'CHKPAGE'),
    ('CHKP27H1', 'ATM', 'CHKPBAL'), ('CHKP27H2', 'ATM', 'CHKPBAL'),
    ('CHKP27H1', 'OVERDRAFT', 'CHKPTENURE'), ('CHKP27H2', 'OVERDRAFT', 'CHKPTENURE'),
    ('SAV27H1', 'MONTHLY', 'PREMIUM'), ('SAV27H2', 'MONTHLY', 'PREMIUM'),
    ('SAV27H1', 'MONTHLY', 'ESTABLISHED'), ('SAV27H2', 'MONTHLY', 'ESTABLISHED'),
    ('SAV27H1', 'ANNUAL', 'SAVAGE'), ('SAV27H2', 'ANNUAL', 'SAVAGE'),
    ('SAV27H1', 'ATM', 'SAVBAL'), ('SAV27H2', 'ATM', 'SAVBAL'),
    ('SAV27H1', 'OVERDRAFT', 'SAVTENURE'), ('SAV27H2', 'OVERDRAFT', 'SAVTENURE'),
    ('MMS27H1', 'MONTHLY', 'PREMIUM'), ('MMS27H2', 'MONTHLY', 'PREMIUM'),
    ('CD27H1', 'EARLYWDRAW', 'SENIOR'), ('CD27H2', 'EARLYWDRAW', 'SENIOR'),
    ('AUTO27H1', 'ORIGIN', 'INCOME'), ('AUTO27H2', 'ORIGIN', 'INCOME'),
    ('AUTO27H1', 'ORIGIN', 'AUTOPAY'), ('AUTO27H2', 'ORIGIN', 'AUTOPAY'),
    ('PERS27H1', 'PROCESS', 'INCOME'), ('PERS27H2', 'PROCESS', 'INCOME'),
    ('MORT27H1', 'ORIGIN', 'LOWLTV'), ('MORT27H1', 'PROCESS', 'LOWLTV'),
    ('MORT27H2', 'ORIGIN', 'LOWLTV'), ('MORT27H2', 'PROCESS', 'LOWLTV')
) as assignments(plan_code, fee_code, reason_code)
join pricing_plans plan on plan.plan_code = assignments.plan_code
join fees fee on fee.fee_code = assignments.fee_code
join eligibility_reasons reason on reason.reason_code = assignments.reason_code;
