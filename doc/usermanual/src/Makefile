MKDOC=../../docbookEnv/mkdoc.sh
OUT_DIR=../output

all: pdf chunk

pdf: $(OUT_DIR)/pdf/eg250.pdf

chunk: $(OUT_DIR)/chunk/index.html

clean: chunk_clean pdf_clean
	rm -rf $(OUT_DIR)

$(OUT_DIR)/pdf/eg250.pdf: eg250.xml
	$(MKDOC) -t pdf -o $(OUT_DIR) -s eg250.xml
	mv $(OUT_DIR)/pdf/output.pdf $(OUT_DIR)/pdf/eg250.pdf

$(OUT_DIR)/chunk/index.html: eg250.xml
	$(MKDOC) -t chunk -o $(OUT_DIR) -s eg250.xml

chunk_clean:
	rm -rf $(OUT_DIR)/chunk


pdf_clean:
	rm -rf $(OUT_DIR)/pdf

   
