@echo off
setlocal enabledelayedexpansion
if not exist "nuget.exe" (
	echo ERROR: Could not find nuget.exe. Please download from https://www.nuget.org/downloads and place in %~dp0
	echo.
	pause
	exit 1
) 

if not exist "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\IDE\devenv.com" (
	echo ERROR: Could not find Visual Studio 2019 Community Edition. Please download from https://visualstudio.microsoft.com/downloads and install the .NET desktop development workload.
	echo.
	pause
	exit 1
) 


echo.
echo Handling nuget-based Relativity Client versions
for %%v in (10.2 10.1 10.0 9.7) do (
	echo.
	echo Building client version %%v 
	robocopy Src Clients\%%v *.cs /S /NFL /NDL /NJH /NJS /NC /NS /NP
	nuget.exe restore Clients\%%v\Client.sln < accept.txt
	"C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\IDE\devenv.com" "Clients\%%v\Client.sln" /build Release
)

echo.
echo Handling SDK-based Relativity Client versions
for %%v in (9.6 9.5 9.4) do (
	echo.
	echo Building client version %%v 
	robocopy Src Clients\%%v *.cs /S /NFL /NDL /NJH /NJS /NC /NS /NP
	nuget.exe restore Clients\%%v\Client.sln < accept.txt
	if not exist "Clients\%%v\Relativity SDK\ImportAPI\Client\x64\kCura.Relativity.ImportAPI.dll" (
		echo.
		echo WARNING: Skipping client version %%v.
		echo Could not find Relativity SDK %%v. Please download SDK from https://platform.relativity.com/%%v/Content/Downloads/Download_the_SDKs.htm
		echo Then, install the SDK and copy contents of folder "C:\Program Files\kCura Corporation\Relativity SDK" to "%~dp0Clients\%%v\Relativity SDK"
		echo.
		pause                      
	) else (

		echo Checking SDK version
		set "item=%~dp0Clients\%%v\Relativity SDK\ImportAPI\Client\x64\kCura.Relativity.ImportAPI.dll"
		set "item=!item:\=\\!"
		for /f "usebackq delims=" %%a in (`"WMIC DATAFILE WHERE name='!item!' get Version /format:Textvaluelist"`) do (
			for /f "delims=" %%# in ("%%a") do (
				if %%# neq "" (
					set versionString=%%#				
					set versionString=!versionString:~8,3!
				)
				set "%%#"
			)
		)		
		if DEFINED versionString (
			echo Detected !versionString!
			if !versionString! neq %%v (
				echo.		
				echo WARNING: Skipping client version %%v.
				echo Relativity SDK version mismatch, expected %%v but found !versionString!
				echo.
				pause				
			) else (
				"C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\IDE\devenv.com" "Clients\%%v\Client.sln" /build Release
			)
		) else (
			echo.
			echo WARNING: Skipping client version %%v.
			echo Could not detect Relativity SDK version
			echo.
			pause
		)
	)
)
