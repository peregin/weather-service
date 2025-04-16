create table forecast(
    "location" varchar2(100) not null,
    "update_time" TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    "data" CLOB NOT NULL CHECK ("data" IS JSON),
    primary key(location, update_time)
) segment creation immediate
    nocompress logging
    tablespace "WEATHER";

create table weather(
    "location" varchar2(100) not null,
    "data" CLOB NOT NULL CHECK ("data" IS JSON),
    primary key(location)
) segment creation immediate
    nocompress logging
    tablespace "WEATHER";

comment on table weather is 'stores the current weather conditions';
comment on table forecast is 'stores the future - forecasted - weather conditions';

