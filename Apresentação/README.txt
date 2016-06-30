    Este é um template básico para apresentações feitas com
o pacote beamer para LaTeX.  Recomendo que você leia o manual
do beamer para usar bem seus recursos.


Instruções básicas
==================

 - Altere o final do arquivo 'header.tex' para alterar seu nome,
   o nome da apresentação, etc.

 - Altere o arquivo 'slides.tex' para colocar seus slides.

 - Para criar o PDF com seus slides, vá para a pasta 'apresentacao'
   e execute

    $ pdflatex apres-beamer.tex

 - Para imprimir os slides, vá para a pasta 'apresentacao' e execute

    $ pdflatex apres-handout.tex

 - Use o 'pdflatex', o template não funciona com 'latex' + 'dvipdf'.
   Se preferir o 'xelatex', edite os arquivos *.tex comentando as
   partes "LaTeX" e descomentando as partes "XeLaTeX".



                                                    -- Felipe Lessa
