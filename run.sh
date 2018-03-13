rm -rf build
mkdir build
cd build
cmake ../c
make
cd ..
cp build/libjnisample.so inaos-example-app/src/main/java/resources
