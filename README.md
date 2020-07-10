# Calibre Viewer

[![](https://img.shields.io/badge/-Android%20APK-green.svg?logo=Android&labelColor=7A7A7A&logoColor=white)](../../releases/)
[![](https://img.shields.io/badge/-Donate-orange.svg?logo=Patreon&labelColor=7A7A7A)](https://www.patreon.com/bePatron?c=954360)
[![](https://img.shields.io/badge/-Donate-blue.svg?logo=Paypal&labelColor=7A7A7A)](https://paypal.me/TSedlar)

<b>A mobile application used to view and download a Calibre library</b>

An internal [fork](https://github.com/TSedlar/FolioReader-Android) of [FolioReader](https://github.com/FolioReader/FolioReader-Android) is included for a FOSS option of reading eBooks.

An external reader, such as Moon+ Reader, can optionally be used instead.

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
- Currently files are only downloaded as epub/zip, we need to add:
  - MIMETYPE to file extension library
  - (Does this matter? The intent is sent with the MIMETYPE, is a file extension forced in reader apps?)
- Backup/Restore functionality
