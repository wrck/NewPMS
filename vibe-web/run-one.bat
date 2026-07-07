@echo off
cd /d "d:\常规软件\AICoding\Trae\workspace\ServiceDeliver\vibe-web"
call npx vitest run src/views/system/__tests__/menu.spec.ts --reporter=verbose --no-file-parallelism > test-one.log 2>&1
echo Done >> test-one.log
