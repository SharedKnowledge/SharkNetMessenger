rm -rf tempLibBuildFolder
mkdir tempLibBuildFolder
cd tempLibBuildFolder
jar -x -f ../libs/ASAPJava.jar
jar -x -f ../libs/ASAPHub.jar
jar -x -f ../libs/SharkPeer.jar
jar -x -f ../libs/SharkPKI.jar
rm -r META-INF/
mkdir META-INF/
echo Manifest-Version: 1.0 > META-INF/MANIFEST.MF
echo Main-Class: net.sharksystem.ui.messenger.cli.ProductionUI >> META-INF/MANIFEST.MF
echo Created-By: Shared Knowledge >> META-INF/MANIFEST.MF
cp -r ../out/production/SharkMessenger/* .
jar cmvf META-INF/MANIFEST.MF SharkNetMessengerCLI.jar *
cd ..
mv tempLibBuildFolder/SharkNetMessengerCLI.jar .
rm -r tempLibBuildFolder/
mkdir playground
mkdir __bugfixing
cp SharkNetMessengerCLI.jar playground/
cp SharkNetMessengerCLI.jar __bugfixing/

