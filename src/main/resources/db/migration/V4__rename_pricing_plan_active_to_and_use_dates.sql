alter table pricing_plans
    rename column active_to to active_through;

alter table pricing_plans
    alter column active_from type date using active_from::date,
    alter column active_through type date using active_through::date;
