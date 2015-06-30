rem simple batch file to automate standard latex build with bib entries
rem step 1: build latex and bibtex files. Need multiple latex runs to 
rem 		resolve all references. Creates .dvi file.
latex %1.tex
bibtex %1
latex %1.tex
latex %1.tex

rem step 2: convert dvi to ps file. Note, dvips needs to force use of
			letter size, thanks to miktex install defaulting to A4.
dvips -t letter %1.dvi

rem step 3: convert ps file to pdf
ps2pdf %1.ps
