create KEYSPACE bolsa_familia WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

create TABLE bolsa_familia.dados (uf text, periodo timestamp, valor double, nis_favorecido bigint, cod_municipio int, fonte text, nome_favorecido text, nome_municipio text, PRIMARY KEY(uf, periodo, valor, nis_favorecido)) WITH CLUSTERING ORDER BY(periodo ASC, valor DESC);
