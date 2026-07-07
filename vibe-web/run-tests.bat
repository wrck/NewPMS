@echo off
cd /d "d:\常规软件\AICoding\Trae\workspace\ServiceDeliver\vibe-web"
call npx vitest run src/views/system/__tests__/menu.spec.ts src/views/system/__tests__/org.spec.ts src/views/system/__tests__/dict.spec.ts src/views/report/__tests__/cockpit.spec.ts src/views/report/__tests__/project.spec.ts src/views/h5/agent/__tests__/workbench.spec.ts src/views/h5/customer/__tests__/projects.spec.ts --reporter=verbose > test-fixes.log 2>&1
exit /b %ERRORLEVEL%
