rmdir /s /q build
mkdir build
cd build
cmake -G"NMake Makefiles" ../c
call nmake
cd ..
del /Q jam-example-lib\src\main\resources\jnisample.dll
copy build\jnisample.dll jam-example-lib\src\main\resources\win32-amd64\jnisample.dll
copy build\example3.dll jam-example-lib\src\main\resources\win32-amd64\example3.dll
mvn clean install -DskipTests
mvn surefire:test
