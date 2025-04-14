create table forecast(
    location varchar2(100) not null,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT SYSTIMESTAMP,
    data CLOB NOT NULL CHECK (data IS JSON),
    primary key(location, update_time)
);

create table weather(
    location varchar2(100) not null,
    data CLOB NOT NULL CHECK (data IS JSON),
    primary key(location)
);

comment on table weather is 'stores the current weather conditions';
comment on table forecast is 'stores the future - forecasted - weather conditions';

