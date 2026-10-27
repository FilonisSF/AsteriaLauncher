; AsteriaLauncher installer: the last page has two ticks - "desktop shortcut" and "start the launcher".
; Updates are silent (no pages), so a shortcut made once stays as it was.
!macro customFinishPage
  Function AsteriaStart
    ${StdUtils.ExecShellAsUser} $0 "$INSTDIR\${PRODUCT_FILENAME}.exe" "open" ""
  FunctionEnd
  Function AsteriaDesktop
    CreateShortCut "$DESKTOP\${PRODUCT_FILENAME}.lnk" "$INSTDIR\${PRODUCT_FILENAME}.exe"
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
  Delete "$DESKTOP\${PRODUCT_FILENAME}.lnk"
!macroend
