create table location(
    location varchar2(100) primary key,
    latitude numeric(8, 4) not null, -- -90 -> 90
    longitude numeric(8, 4) not null -- -180 -> 80
);

-- insert / migrate existing data from velocorner database
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('linz, at',48.3064,14.2861),
	 ('gorzów wielkopolski',52.7400,15.2300),
	 ('valletta, mt',35.8997,14.5147),
	 ('nairobi, ke',-1.2833,36.8167),
	 ('tunis, tn',34.0000,9.0000),
	 ('gent',51.0500,3.7167),
	 ('guayaquil',-2.1700,-79.9000),
	 ('tehran, ir',35.6944,51.4215),
	 ('india',46.7400,12.2800),
	 ('santiago, cl',-33.4569,-70.6483);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('adliswil',47.3100,8.5246),
	 ('beirut, lb',33.8889,35.4944),
	 ('lahore, pk',31.5497,74.3436),
	 ('astana, kz',51.1801,71.4460),
	 ('mittelberg',47.3300,10.1500),
	 ('clevedon',51.4406,-2.8575),
	 ('pakistan',30.0000,70.0000),
	 ('katni',23.8300,80.3900),
	 ('oporto',41.2500,-8.3333),
	 ('tel aviv',32.0800,34.8000);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('melbourne',28.0836,-80.6081),
	 ('france',5.2000,-3.7400),
	 ('iran',32.0000,53.0000),
	 ('kórnik',52.2477,17.0895),
	 ('krishnagar',23.5700,89.7700),
	 ('havana, cu',23.1330,-82.3830),
	 ('bridgetown, bb',13.1000,-59.6167),
	 ('melegnano',45.3588,9.3240),
	 ('sarajevo, ba',43.8486,18.3564),
	 ('vientiane, la',17.9667,102.6000);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('35013',36.8169,3.8578),
	 ('seoul, kr',37.5683,126.9778),
	 ('almaty',43.2500,76.9500),
	 ('600024',13.0500,80.2000),
	 ('bangkok, th',13.7500,100.5167),
	 ('obada',7.0700,3.2800),
	 ('solothurn',47.2079,7.5371),
	 ('błażejewo, pl',52.2149,17.1002),
	 ('brussels, be',50.8504,4.3488),
	 ('san juan, pr',18.4510,-66.0917);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('hong kong, hk',22.2855,114.1577),
	 ('sofia, bg',42.6975,23.3242),
	 ('bucharest, ro',44.4323,26.1063),
	 ('praia, cv',14.9215,-23.5087),
	 ('pretoria, za',-25.7449,28.1878),
	 ('sutton, surrey',51.3500,-0.2000),
	 ('boortmeerbeek',50.9793,4.5744),
	 ('tokyo, jp',35.6895,139.6917),
	 ('tripura',24.0000,92.0000),
	 ('washington, usmeghalaya',47.5001,-120.5015);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('algiers, dz',36.7525,3.0420),
	 ('zürich',47.3667,8.5500),
	 ('khartoum, sd',15.5518,32.5324),
	 ('chittagong , bd',22.9167,91.5000),
	 ('bla',12.9494,-5.7620),
	 ('blankenberge',51.3131,3.1323),
	 ('tbilisi, ge',41.6941,44.8337),
	 ('riyadh, sa',24.6877,46.7219),
	 ('mumbai',19.0144,72.8479),
	 ('victoria, sc',-4.6167,55.4500);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('zagreb, hr',45.8131,15.9775),
	 ('washington, us parsa',47.5001,-120.5015),
	 ('luxembourg, lu',49.6117,6.1300),
	 ('kathmandu, np',27.7167,85.3167),
	 ('colombo, lk',6.9319,79.8478),
	 ('zwolle, nl',52.5126,6.0936),
	 ('ljubljana, si',46.0511,14.5051),
	 ('kingstown, vc',13.1587,-61.2248),
	 ('abuja, ng',9.0574,7.4898),
	 ('la réunion',-21.1000,55.6000);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('manila, ph',14.6042,120.9822),
	 ('kassel',51.3167,9.5000),
	 ('athens, gr',37.9795,23.7162),
	 ('kigali, rw',-1.9500,30.0588),
	 ('girona',41.9831,2.8249),
	 ('geneva',41.8875,-88.3054),
	 ('torrent, es',39.4371,-0.4655),
	 ('caracas, ve',10.4880,-66.8792),
	 ('yerevan, am',40.1811,44.5136),
	 ('kuwait city, kw',29.3697,47.9783);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('fiesch, ch',46.4004,8.1311),
	 ('abu dhabi, ae',24.4667,54.3667),
	 ('copenhagen, dk',55.6759,12.5655),
	 ('rabat, ma',33.9911,-6.8401),
	 ('cairo, eg',30.0626,31.2497),
	 ('vienna, at',48.2085,16.3721),
	 ('tampico',22.2167,-97.8500),
	 ('damascus, sy',33.5102,36.2913),
	 ('bangalore',12.9762,77.6033),
	 ('den bosch',51.5838,6.0202);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('cittadella',45.6500,11.7842),
	 ('belgrade, rs',44.8040,20.4651),
	 ('smolensk',55.0000,33.0000),
	 ('baia mare',47.6533,23.5795),
	 ('penza',53.2007,45.0046),
	 ('navi mumbai',19.0368,73.0158),
	 ('wellington, nz',-41.2866,174.7756),
	 ('odessa, ua',46.4775,30.7326),
	 ('ankara, tr',39.9199,32.8543),
	 ('dhaka, bd',23.7104,90.4074);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('jakarta, id',-6.2146,106.8451),
	 ('amman, jo',31.9552,35.9450),
	 ('guatemala city, gt',14.6407,-90.5133),
	 ('podgorica, me',42.4411,19.2636),
	 ('hertogenbosch',51.6992,5.3042),
	 ('reykjavik, is',64.1355,-21.8954),
	 ('lisbon, pt',38.7167,-9.1333),
	 ('baghdad, iq',33.3406,44.4009),
	 ('vilnius, lt',54.6892,25.2798),
	 ('suva, fj',-18.1416,178.4415);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('bogota, co',4.6097,-74.0817),
	 ('genf, ch',46.2022,6.1457),
	 ('helsinki, fi',60.1695,24.9355),
	 ('lenzerheide, ch',46.7279,9.5568),
	 ('dublin, ie',53.3440,-6.2672),
	 ('mexico city, mx',19.4285,-99.1277),
	 ('zurich,ch',47.3667,8.5500),
	 ('phnom penh, kh',11.5625,104.9160),
	 ('bishkek, kg',42.8700,74.5900),
	 ('finale ligure',44.1695,8.3436);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('baar, ch',47.1963,8.5295),
	 ('skopje, mk',42.0000,21.4333),
	 ('juba, ss',4.8517,31.5825),
	 ('kinshasa, cd',-4.3246,15.3215),
	 ('dresden',51.0509,13.7383),
	 ('siófok,hu',46.9041,18.0580),
	 ('lingen, de',52.5167,7.3167),
	 ('apeldoorn',52.2100,5.9694),
	 ('zielona góra',51.9355,15.5064),
	 ('singapur, sg',1.2897,103.8501);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('taipei, tw',25.0478,121.5319),
	 ('buenos aires, ar',-34.6132,-58.3772),
	 ('melbourne, au',-37.8140,144.9633),
	 ('ottawa, ca',45.4112,-75.6981),
	 ('nicosia, cy',35.1667,33.3667),
	 ('stockholm, se',59.3326,18.0649),
	 ('moscow, ru',55.7522,37.6156),
	 ('brasilia, br',-15.7797,-47.9297),
	 ('kiev, ua',50.4333,30.5167),
	 ('prague, cz',50.0880,14.4208);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('tallinn, ee',59.4370,24.7535),
	 ('castries, lc',13.9957,-61.0061),
	 ('port louis, mu',-20.1619,57.4989),
	 ('slupsk',54.4641,17.0287),
	 ('minsk, by',53.9000,27.5667),
	 ('dushanbe, tj',38.5577,68.7797),
	 ('chur, ch',46.8499,9.5329),
	 ('baia mare, ro',47.6533,23.5795),
	 ('yamoussoukro, ci',6.8206,-5.2767),
	 ('wokingham',51.4112,-0.8357);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('belmopan, bz',17.2500,-88.7667),
	 ('adliswil,ch',47.3100,8.5246),
	 ('heidelberg, de',49.4077,8.6908),
	 ('andermatt',46.6357,8.5939),
	 ('siofok,how',46.9041,18.0580),
	 ('siófok',46.9041,18.0580),
	 ('sao paulo',-23.5475,-46.6361),
	 ('chisinau, md',47.0056,28.8575),
	 ('cape town',-33.9258,18.4232),
	 ('madrid, es',40.4165,-3.7026);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('denderleeuw',50.8875,4.0593),
	 ('słupsk',54.4641,17.0287),
	 ('seriate',45.6834,9.7229),
	 ('tegucigalpa, hn',14.0818,-87.2068),
	 ('katowice',50.2584,19.0275),
	 ('xativa',38.9833,-0.5167),
	 ('bratislava, sk',48.1482,17.1067),
	 ('noumea, nc',-22.2763,166.4572),
	 ('gorzów wielkopolski, pl',52.7368,15.2288),
	 ('radwanów',51.0385,20.1560);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('budapest',47.4980,19.0399),
	 ('münchen, de',48.1374,11.5755),
	 ('montevideo, uy',-34.8335,-56.1674),
	 ('września',52.3251,17.5652),
	 ('male, mv',4.1748,73.5089),
	 ('hamilton, bm',32.2915,-64.7780),
	 ('jerusalem, il',31.7690,35.2163),
	 ('gibraltar, gi',36.1447,-5.3526),
	 ('chełmża',53.1846,18.6047),
	 ('riga, lv',57.0000,24.0833);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('paris',48.8534,2.3488),
	 ('thane',19.2000,72.9667),
	 ('basseterre, kn',17.2948,-62.7261),
	 ('amsterdam, nl',52.3740,4.8897),
	 ('kingston, jm',17.9970,-76.7936),
	 ('hungary',47.0000,20.0000),
	 ('bezons',48.9333,2.2167),
	 ('winterthur, ch',47.5000,8.7500),
	 ('ulan bator, mn',47.9077,106.8832),
	 ('papeete, pf',-17.5333,-149.5667);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('somerset west',-34.0840,18.8211),
	 ('kütahya',39.2500,29.5000),
	 ('warsaw, pl',52.2298,21.0118),
	 ('bilbao, es',43.2627,-2.9253),
	 ('bergamo',45.6980,9.6690),
	 ('luzern,ch',47.0505,8.3064),
	 ('tirana, al',41.3275,19.8189),
	 ('malaga',36.7202,-4.4203),
	 ('little rock',34.7465,-92.2896),
	 ('basel, ch',47.5584,7.5733);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('malaga, es',36.7202,-4.4203),
	 ('gorontalo',0.5412,123.0595),
	 ('gorontalo, id',0.5412,123.0595),
	 ('einsiedeln, ch',47.1167,8.7500),
	 ('saipan, mp',15.1355,145.7010),
	 ('ravenna, it',44.4167,11.9833),
	 ('saint-pierre, pm',46.7654,-56.1695),
	 ('islamabad, pk',33.7104,73.1338),
	 ('girona, es',41.9831,2.8249),
	 ('lublin, pl',51.0000,23.0000);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('palermo, it',37.8167,13.5833),
	 ('panama city, pa',8.9936,-79.5197),
	 ('rueschlikon',47.3043,8.5469),
	 ('sibiu, ro',45.8000,24.1500),
	 ('sadu, ro',45.6667,24.1833),
	 ('lusaka, zm',-15.4067,28.2871),
	 ('zurich, ch',47.3667,8.5500),
	 ('maputo, mz',-25.9653,32.5892),
	 ('quito, ec',-0.2299,-78.5249),
	 ('gniezno, pl',52.5348,17.5826);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('hanoi, vn',21.0245,105.8412),
	 ('kuala lumpur, my',3.1431,101.6865),
	 ('wrocław',51.1000,17.0333),
	 ('ollon',46.2952,6.9931),
	 ('wrocław, pl',51.1000,17.0333),
	 ('baku, az',40.3777,49.8920),
	 ('vaduz, li',47.1415,9.5215),
	 ('port of spain, tt',10.6662,-61.5166),
	 ('oslo, no',59.9127,10.7461),
	 ('asuncion, py',-25.3007,-57.6359);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('sakarya',40.7500,30.5833),
	 ('nyon',46.3832,6.2396),
	 ('aalborg, dk',57.0480,9.9187),
	 ('china',25.7000,-99.2333),
	 ('nantes',47.1667,-1.5833),
	 ('ulm, de',48.3984,9.9916),
	 ('buena vista',43.4203,-83.8986),
	 ('gustavia, bl',17.8962,-62.8498),
	 ('zürich, ch',47.3667,8.5500),
	 ('canberra, au',-35.2835,149.1281);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('budapest, hu',47.4980,19.0399),
	 ('new delhi, in',28.6128,77.2311),
	 ('rome, it',41.8947,12.4839),
	 ('fushun',41.8558,123.9233),
	 ('paris, fr',48.8534,2.3488),
	 ('adliswil, ch',47.3100,8.5246),
	 ('berlin, de',52.5244,13.4105),
	 ('beijing, cn',39.9075,116.3972),
	 ('lugano, ch',46.0101,8.9600),
	 ('washington, us',47.5001,-120.5015);
INSERT INTO public."location" ("location",latitude,longitude) VALUES
	 ('berne, ch',46.9481,7.4474);
