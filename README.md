# Calibre Viewer

[![](https://img.shields.io/badge/-Android%20APK-green.svg?logo=Android&labelColor=7A7A7A&logoColor=white)](../../releases/)
[![](https://img.shields.io/badge/-Donate-orange.svg?logo=Patreon&labelColor=7A7A7A)](https://www.patreon.com/bePatron?c=954360)
[![](https://img.shields.io/badge/-Donate-blue.svg?logo=Paypal&labelColor=7A7A7A)](https://paypal.me/TSedlar)

A mobile application used to view/download from a Calibre library

A reading application is needed along-side this application.

This application is solely used for visual appeal and offline library viewing.

## Screenshots

<p>
  <img src="wiki/screenshots/Screenshot_20200628-110703_Calibre_Viewer.png" width="300" />
  <img src="wiki/screenshots/Screenshot_20200628-110709_Calibre_Viewer.png" width="300" />
  <img src="wiki/screenshots/Screenshot_20200628-110717_Calibre_Viewer.png" width="300" />
  <img src="wiki/screenshots/Screenshot_20200628-110727_Calibre_Viewer.png" width="300" />
  <img src="wiki/screenshots/Screenshot_20200628-110912_Calibre_Viewer.png" width="300" />
  <img src="wiki/screenshots/Screenshot_20200628-110926_Calibre_Viewer.png" width="300" />
  <img src="wiki/screenshots/Screenshot_20200628-111019_Calibre_Viewer.png" width="300" />
</p>


## Plans

- The series cover view changer needs to be implemented
- Currently only series view is supported, could add support for (as a list view):
  - Newest
  - Title
  - Author
  - Languages
  - Publisher
  - Tags
- Currently files are only downloaded as epub/zip, we need to add:
  - MIMETYPE to file extension library
  - (Does this matter? The intent is sent with the MIMETYPE, is a file extension forced in reader apps?)
- A download queue is needed
  - Will enable us to add mass downloading
  - Also need to add mass deleting when we add this
- Backup/Restore functionality