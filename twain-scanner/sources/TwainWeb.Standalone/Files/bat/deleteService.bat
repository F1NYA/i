@echo off
@echo ����塞 �ࢨ�
@echo ====================
@echo.
sc query | findstr /i "TWAIN@Web"

IF ERRORLEVEL 1 (GOTO NOTEXIST) ELSE GOTO EXIST
:NOTEXIST
@echo ��ࢨ� �� �������
GOTO ENDGOTO

:EXIST

::�����஢騪 ����� ����� ������� SC QUERY schedule, � �� �㤥� ࠡ���� �����. 
::�஢����, ����饭� �� �㦡�, ����� �� ������ ��ப� RUNNING ��� STOPPED:
sc query schedule | find "RUNNING"

sc stop TWAIN@Web
sc delete TWAIN@Web
@echo �ᯥ� :) �ࢨ� �� 㤠���

:ENDGOTO
@echo.


@pause