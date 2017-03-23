## Наличие в любых IDшниках кириллических символов, а также пробелов - недопустимо

## переменные

первый символ названия переменной должен говорить о типе данных:
* n = Number (числовые переменные) - nSum
* s = String (строчные переменные) - sFamily
* a = Array (массивы) - anPrice, asName, aDepartament
* o = Object (обьекты/когда трудно определить строка или число) - oVisitDate
* b = boolean 

Для элементов enum помимо id доступно поле name.
например (скоро будет реализовано):
name="name;13,40;decimal;depCode=dep9345,depNum=44"

где:
* 1-й разряд = название (выводится в интерфейс)
* 2-й разряд = значение //опционально
* 3-й разряд = тип //опционально (те-же типы, что и в активити)
* 4-й разряд = набор присвоений переменных, через запятую //опционально

## бизнес-процеcсы
Наименование процесса должно строиться по принципу  
`{орган}_{номер услуги в Service}_{суть}_{приставка}`  
{суть} и {приставка} - опционально (т.е. не обязательно)  
{номер услуги в Service} - будет всегда состоять из 4х цифр, чтоб была правильная сортировка  
приставка нужна для того, что если у Вас на один и тот же номер сервиса будет несколько бп  
Наименование и ID процесса должны совпадать.
например
  
zags_0705_changeName   
zags_0710_death  

eco_0520_auditori  
eco_0521  
* ![4_0](https://github.com/e-government-ua/i/blob/test/doc/bp/img/4_0.JPG)
### {орган} Приставки для именования БП
**dfs** - налоговая  
**dms** - миграционная  
**dpss** - ДержПродСпоживСлужба (екс-СЕС)  
**zem** - услуги по земле  
**eco** - экология  
**zags** - загсы  
**upszn** - услуги соц.помощи  
**justice** - юстиция  
**infrastr** - услуги мин.инфраструктуры  
**rvk** - военкомат  
**dvs** - исполнительная служба  
**kids** - служба по делам детей  
**oda** - услуги ОДА  
**rada** - услуги местных органов власти (гор.советы, сельские советы, районные рады, поселковые советы)  
**med** - медицинские услуги  
* ![4_1](https://github.com/e-government-ua/i/blob/test/doc/bp/img/4_1.JPG)
## группы и пользователи
## принтформы
## выносные файлы

## Подсвечивать этап (юзертаску) в дашборде  цветом

если название юзертаски заканчивается на:
* "_red" - подкрашивать строку - красным цветом (класс: "bg_red")
* "_yellow" - подкрашивать строку - желтым цветом (класс: "bg_yellow")
* "_green" - подкрашивать строку - зеленым цветом (класс: "bg_green")
* "usertask1" - подкрашивать строку - салатовым цветом (класс: "bg_first")