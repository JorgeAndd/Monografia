import os, random 


def get_random_line(file_name):
	total_bytes = os.stat(file_name).st_size 
	random_point = random.randint(0, total_bytes)
	file = open(file_name)
	file.seek(random_point)
	file.readline() # skip this line to clear the partial line
	return file.readline()

files = [
	"201407_BolsaFamiliaFolhaPagamento.csv",
	"201408_BolsaFamiliaFolhaPagamento.csv",
	"201409_BolsaFamiliaFolhaPagamento.csv",
	"201410_BolsaFamiliaFolhaPagamento.csv",
	"201411_BolsaFamiliaFolhaPagamento.csv",
	"201412_BolsaFamiliaFolhaPagamento.csv"
];

print('starting...');
f = open('queries.txt', 'w')
for file_name in files:
	for i in range(0,10):
		line = get_random_line(file_name)
		fields = line.split('\t')

		uf = fields[0]
		cod_municipio = fields[1]
		nome_municipio = fields[2]
		cod_funcao = fields[3]
		cod_subfuncao = fields[4]
		cod_programa = fields[5]
		cod_acao = fields[6]
		nis_favorecido = fields[7]
		nome_favorecido = fields[8]
		fonte = fields[9]
		valor = fields[10]
		mes, ano = fields[11].strip().split('/')

		periodo = (ano + '-' + mes + '-01')

		cmd = "SELECT * FROM dados WHERE uf = '%s' AND periodo = '%s' AND valor = %s AND nis_favorecido = %s\n" % (uf, periodo, valor, nis_favorecido)

		f.write(cmd)

