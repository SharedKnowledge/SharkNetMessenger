rmdir /s /q tempLibBuildFolder
mkdir tempLibBuildFolder
cd tempLibBuildFolder
jar -x -f ..\libs\ASAPJava.jar
jar -x -f ..\libs\ASAPHub.jar
jar -x -f ..\libs\SharkPeer.jar
jar -x -f ..\libs\SharkPKI.jar
rmdir /s /q META-INF
mkdir META-INF
echo Manifest-Version: 1.0 > META-INF\MANIFEST.MF
echo Main-Class: net.sharksystem.ui.messenger.cli.ProductionUI >> META-INF\MANIFEST.MF
echo Created-By: Shared Knowledge >> META-INF\MANIFEST.MF
xcopy /S ..\out\production\SharkMessenger\* .
jar cmvf META-INF/MANIFEST.MF SharkNetMessengerCLI.jar *
cd ..
move tempLibBuildFolder\SharkNetMessengerCLI.jar .
rmdir /s /q tempLibBuildFolder
md playground
md __bugfixing
copy SharkNetMessengerCLI.jar playground
copy SharkNetMessengerCLI.jar __bugfixing
copy SharkNetMessengerCLI.jar playground2
