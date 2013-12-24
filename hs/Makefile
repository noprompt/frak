all: clean test
	cabal build frak

clean:
	rm -rf dist/build

test: clean
	cabal build test-frak
	./dist/build/test-frak/test-frak
