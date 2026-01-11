rm -rf *.jar

mvn clean package;
mv target/IoUringSendZC-1.0-SNAPSHOT.jar IoUringSendZC_patch.jar

mvn clean package -Poriginal-netty;
mv target/IoUringSendZC-1.0-SNAPSHOT.jar IoUringSendZC_old.jar