@echo off

if "%1"=="" goto usage
if "%1"=="/h" goto usage
if "%1"=="/?" goto usage
goto :begin

:usage
echo Usage:
echo %0 [OPTIONS], where options can be:
echo 	--domain=DOMAIN	sets the XMPP domain
echo    --password=specifies the password used by focus XMPP user to login. If not provided then focus user will use anonymous authentication method.
echo 	--host=HOST	sets the hostname of the XMPP server (default: domain, if domain is set, localhost otherwise)
echo 	--port=PORT	sets the port of the XMPP server (default: 5347)
echo    --subdomain=SUBDOMAIN sets the sub-domain used to bind focus XMPP component (default: focus)
echo	--secret=SECRET	sets the shared secret used to authenticate focus component to the XMPP server
echo.
exit /B 1

:begin

:: needed to overcome weird loop behavior in conjunction with variable expansion
SETLOCAL enabledelayedexpansion

set mainClass=org.jitsi.jicofo.Main
set cp=jicofo.jar
FOR %%F IN (lib/*.jar) DO (
  SET cp=!cp!;lib/%%F%
)

java -Djava.library.path=lib/native/windows-64 -Djava.util.logging.config.file=lib/logging.properties -cp %cp% %mainClass% %*