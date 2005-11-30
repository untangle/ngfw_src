MKDOC=../../../../mvdocbook/mkdoc.sh
OUT_DIR=../output

all: pdf chunk

pdf: $(OUT_DIR)/pdf/eg250.pdf

chunk: $(OUT_DIR)/chunk/index.html

clean: chunk_clean pdf_clean
	rm -rf $(OUT_DIR)

$(OUT_DIR)/pdf/eg250.pdf: eg250.xml
	$(MKDOC) -v -t pdf -o $(OUT_DIR) -s eg250.xml -x stylesheets/eg_manual_fo.xsl
	mv $(OUT_DIR)/pdf/output.pdf $(OUT_DIR)/pdf/eg250.pdf

$(OUT_DIR)/chunk/index.html: eg250.xml
	$(MKDOC) -v -t chunk -o $(OUT_DIR) -s eg250.xml -c stylesheets/metavize.css -x stylesheets/eg_manual_chunk.xsl

chunk_clean:
	rm -rf $(OUT_DIR)/chunk

pdf_clean:
	rm -rf $(OUT_DIR)/pdf

   
