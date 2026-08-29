insert into branches (branch_code, branch_name, state, zip_code, updated_on, updated_by)
values
    ('BR100', 'Downtown Branch', 'NY', '10001', current_timestamp, 'seed'),
    ('BR101', 'Harbor Branch', 'NY', '11201', current_timestamp, 'seed'),
    ('BR200', 'Lakeside Branch', 'IL', '60601', current_timestamp, 'seed'),
    ('BR201', 'Northside Branch', 'IL', '60611', current_timestamp, 'seed'),
    ('BR300', 'Hill Country Branch', 'TX', '78701', current_timestamp, 'seed'),
    ('BR301', 'Riverwalk Branch', 'TX', '78205', current_timestamp, 'seed');

insert into products (product_code, product_name, product_type, updated_on, updated_by)
values
    ('CHK', 'Everyday Checking', 'DEPOSIT', current_timestamp, 'seed'),
    ('CHKPLUS', 'Premium Checking', 'DEPOSIT', current_timestamp, 'seed'),
    ('SAV', 'High Yield Savings', 'DEPOSIT', current_timestamp, 'seed'),
    ('MMSAV', 'Money Market Savings', 'DEPOSIT', current_timestamp, 'seed'),
    ('CD12', '12 Month Certificate', 'CD', current_timestamp, 'seed'),
    ('CD3', '3 Month Certificate', 'CD', current_timestamp, 'seed'),
    ('AUTO', 'Auto Loan', 'CREDIT', current_timestamp, 'seed'),
    ('PERS', 'Personal Loan', 'CREDIT', current_timestamp, 'seed'),
    ('MORT', 'Home Mortgage', 'CREDIT', current_timestamp, 'seed');

insert into regions (region_code, region_name, updated_on, updated_by)
values
    ('EAST', 'Eastern Region', current_timestamp, 'seed'),
    ('MID', 'Midwest Region', current_timestamp, 'seed'),
    ('SOUTH', 'Southern Region', current_timestamp, 'seed');

insert into region_branches (region_id, branch_id)
select region.id, branch.id
from regions region
join branches branch on (region.region_code, branch.branch_code) in (
    ('EAST', 'BR100'), ('EAST', 'BR101'), ('MID', 'BR200'),
    ('MID', 'BR201'), ('SOUTH', 'BR300'), ('SOUTH', 'BR301')
);

insert into region_states (region_id, state_code)
select region.id, state_code
from regions region
join (values ('EAST', 'NY'), ('MID', 'IL'), ('SOUTH', 'TX')) as states(region_code, state_code)
    on states.region_code = region.region_code;

insert into region_zip_codes (region_id, zip_code)
select region.id, zip_code
from regions region
join (values
    ('EAST', '10001'), ('EAST', '11201'), ('MID', '60601'),
    ('MID', '60611'), ('SOUTH', '78701'), ('SOUTH', '78205')
) as zip_codes(region_code, zip_code) on zip_codes.region_code = region.region_code;
