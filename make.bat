rmdir /s /q build
mkdir build
cd build
cmake -G"NMake Makefiles" ../c
call nmake
cd ..
del /Q inaos-example-lib\src\main\resources\jnisample.dll
copy build\jnisample.dll inaos-example-lib\src\main\resources\jnisample.dll
mvn clean package -DskipTests
