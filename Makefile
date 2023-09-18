BUILD_DIR := build

SRC_DIR := src/main/kotlin

PKG := klox

SOURCES := $(wildcard  $(SRC_DIR)/*.kt)
#CLASSES := $(addprefix $(BUILD_DIR)/$(PKG)/, $(notdir $(SOURCES:.kt=.class)))

KFLANGS := -Werror  -include-runtime -no-reflect

run: build
	@ echo "klox running $(script)"
	@ kotlin -classpath $(BUILD_DIR)/$(PKG).jar  klox.KloxKt $(script)

build: $(BUILD_DIR)/$(PKG).jar
	@:

$(BUILD_DIR)/$(PKG).jar:
	@ mkdir -p $(BUILD_DIR)
	@ kotlinc $(SOURCES)  $(KFLANGS)  -d $(BUILD_DIR)/$(PKG).jar

clean: 
	@ rm -fr build/

.PHONY: build  run  clean
