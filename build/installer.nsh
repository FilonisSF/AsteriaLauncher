; AsteriaLauncher installer: the last page has two ticks - "desktop shortcut" and "start the launcher".
; Updates are silent (no pages) and run the old uninstaller first, so the choice is kept in the registry and the
; shortcut is made again after every update (fresh target and icon); "0" = the player did not want one.
!macro customInstall
  ${if} ${isUpdated}
    ReadRegStr $1 HKCU "Software\AsteriaLauncher" "DesktopShortcut"
    ${if} $1 != "0"
      CreateShortCut "$DESKTOP\${PRODUCT_FILENAME}.lnk" "$INSTDIR\${PRODUCT_FILENAME}.exe"
      WriteRegStr HKCU "Software\AsteriaLauncher" "DesktopShortcut" "1"
    ${endif}
  ${else}
    WriteRegStr HKCU "Software\AsteriaLauncher" "DesktopShortcut" "0"
  ${endif}
!macroend
!macro customFinishPage
  Function AsteriaStart
    ${StdUtils.ExecShellAsUser} $0 "$INSTDIR\${PRODUCT_FILENAME}.exe" "open" ""
  FunctionEnd
  Function AsteriaDesktop
    CreateShortCut "$DESKTOP\${PRODUCT_FILENAME}.lnk" "$INSTDIR\${PRODUCT_FILENAME}.exe"
    WriteRegStr HKCU "Software\AsteriaLauncher" "DesktopShortcut" "1"
  FunctionEnd
  !define MUI_FINISHPAGE_RUN
  !define MUI_FINISHPAGE_RUN_TEXT "Запустить AsteriaLauncher"
  !define MUI_FINISHPAGE_RUN_FUNCTION "AsteriaStart"
  !define MUI_FINISHPAGE_SHOWREADME
  !define MUI_FINISHPAGE_SHOWREADME_TEXT "Создать ярлык на рабочем столе"
  !define MUI_FINISHPAGE_SHOWREADME_FUNCTION "AsteriaDesktop"
  !insertmacro MUI_PAGE_FINISH
!macroend
!macro customUnInstall
  ; an update runs this too: the shortcut and the choice stay then
  ${ifNot} ${isUpdated}
    Delete "$DESKTOP\${PRODUCT_FILENAME}.lnk"
    DeleteRegKey HKCU "Software\AsteriaLauncher"
  ${endif}
!macroend
