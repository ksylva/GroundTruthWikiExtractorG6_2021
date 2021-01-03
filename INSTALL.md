# Install

Depending on the IDE that you chose, you will have to import the _pom.xml_ file into your project.  
This will allow Maven to automatically import the missing libraries to properly run the project.  
Everything is set now; you are ready to run the program.

*The following installation process is for IntelliJ :*  

Requirements :
- IntelliJ
- Maven
- JavaFX SDK

## JavaFX SDK
Download and unzip, according to your OS, the javafx sdk file in the folder you want.
The download [link](https://gluonhq.com/products/javafx/).

Start by opening IntelliJ and close all opened projects.
Then get the project with the HTTPS [link](https://github.com/ksylva/GroundTruthWikiExtractorG6_2021.git).
  - Click "Check out from Version Control" on IntelliJ
  - Select "GitHub"
  - On "Git Repository URL" put the HTTPS link of this project
  - Set your project directory and parent directory as you wish
  - Click "Clone" when you are ready
  - Open the project
  - Wait until maven import all dependencies
 
 * Create a runtime configuration by following this [link](https://www.jetbrains.com/help/idea/javafx.html#vm-options)
  + After is done, you can click "Run" button of IDE to run the program