# MiniProjekt BedBooker

BedBooker to narzędzie ułatwiające zarządzanie hotelem. Jego głównym zastosowaniem jest obsługa bazodanowa rezerwacji. Projekt tworzony jest przy pomocy MySQL oraz Javy.

---

Imiona i nazwiska autorów : Gabriela Dumańska, Katarzyna Lisiecka

---

# Tabele

<img src="zrzuty_ekranu/Schemat_bazy.png" alt="Schemat bazy danych" width="500"/>

- `Persons`  - osoby

  - `PersonID` - identyfikator, klucz główny
  - `Name` - imię
  - `Surname` - nazwisko
  - `StreetAddress` - adres
  - `CityID` -  identyfikator miasta, klucz obcy
  - `CountryID` -  identyfikator kraju, klucz obcy
  - `PhoneNumber` -  numer telefonu
  - `Email` - adres e-mail

- `Cities` - słownik miast
  - `CityID` - identyfikator, klucz główny
  - `CityName` - nazwa miasta

- `Countries` - słownik państw
  - `CountryID` - identyfikator, klucz główny
  - `CountryName` - nazwa państwa

- `Rooms` - pokoje
  - `RoomID` - identyfikator, klucz główny
  - `NumberOfPlaces` - liczba miejsc
  - `Price` - cena za pokój za dobę hotelową

- `Reservations`  - rezerwacje

  - `ReservationID` - identyfikator, klucz główny
  - `PersonID` - identyfikator osoby, klucz obcy
  - `RoomID` - identyfikator pokoju, klucz obcy
  - `StartDate` - początek pobytu
  - `EndDate` -  koniec pobytu
  - `StatusID` -  identyfikator statusu, klucz obcy
  - `Price` -  cena rezerwacji
  - `Discount` - zniżka

- `Damages` - pokoje
  - `DamageID` - identyfikator, klucz główny
  - `ReservationID` - identyfikator rezerwacji, klucz obcy
  - `Date` - data zniszczenia
  - `Price` - wartość szkód

- `Logs` - dziennik zmian statusów rezerwacji

  - `LogID` - identyfikator, klucz główny
  - `ReservationID` - identyfikator rezerwacji, klucz obcy
  - `StatusID` - identyfikator statusu, klucz obcy
  - `Date` - data zmiany

- `Statuses` - słownik statusów
  - `StatusID` - identyfikator, klucz główny
  - `StatusName` - nazwa statusu- rezerwacja nowa, potwierdzona i zapłacona, anulowana
  
---
<div style="page-break-after: always;"></div>

# Widok administratora
Dla widoku administratora zaprojektowano kluczowe funkcje dla kontrolowania pracy hotelu.

  ## Logowanie do systemu
Widok administratora został zabezpieczony hasłem przed nieporządanymi działaniami.

<img src="zrzuty_ekranu/logowanie.png" alt="Schemat bazy danych" width="500"/>

--- 

  ## Statystyki

<img src="zrzuty_ekranu/statystyki.png" alt="Schemat bazy danych" width="500"/>

  Ten panel pomaga pracownikowi szybko zorietnować się w jakim stanie jest obecnie hotel.

  <div style="page-break-after: always;"></div>

  ### Zarobki z ostatnich 5 miesięcy
  Tę statystykę uzyskano korzystając z następującego widoku:
   
  ```mysql
    CREATE VIEW Earnings AS
      SELECT EXTRACT(YEAR FROM StartDate) AS Rok,
             EXTRACT(MONTH FROM StartDate) AS Miesiąc,
             SUM(Price * ((100 - Discount) / 100)) AS Zarobki
      FROM Reservations
        WHERE StatusID = 3 AND StartDate <= NOW()
        GROUP BY EXTRACT(YEAR FROM StartDate), EXTRACT(MONTH FROM StartDate)
        ORDER BY Rok DESC, Miesiąc DESC
        LIMIT 5;
  ```
  Zarobki to suma płatności za rezerwacje w danym miesiącu. Upewniono się, że liczone są rezerwacje jedynie opłacone- nieoczekujące, ani nieodwołane, czyli te ze statusem 3. Nie liczono również rezerwacji opłaconych, które się jeszcze nie odbyły.

  ### Obecny stan pokoi
  Ten wykres uzyskano z dwóch widoków- liczby pokoi obecni zamieszkałych oraz wszystkich pokoi.
  ```mysql
    CREATE VIEW NumberOfOccupiedRooms AS
      SELECT COUNT(res.ReservationID) AS OccupiedRoomCount
      FROM Rooms r
          LEFT JOIN Reservations res ON r.RoomID = res.RoomID
        WHERE res.StartDate <= CURDATE() AND res.EndDate >= CURDATE();
  ```
  ```mysql
    CREATE VIEW NumberOfRooms AS
      SELECT COUNT(r.RoomID) AS RoomCount
      FROM Rooms r
  ```
  W kolejnym punkcie w razie potrzeby wyciągnięcia dwóch liczb z bazy danych wykonano to w ramach jednego widoku, co uznano za lepsze rozwiązanie. 

<div style="page-break-after: always;"></div>

  ### Poniesione szkody
  Pierwszy wykres kołowy to prosta statystyka unikalnych rezerwacji z tabeli Damages do pozostałych rezerwacji.
  
  ```mysql
    CREATE VIEW DamagesPerReservations AS
      SELECT
      (SELECT COUNT(DISTINCT ReservationID) FROM Damages) AS ReservationsWithDamages,
      (SELECT COUNT(ReservationID) FROM Reservations 
                                   WHERE EndDate < NOW()) AS UniqueReservations;
```
  Takie rozwiązanie wyciągania dwóch liczb uznano za bardziej czytelne- od razu wiadomo do czego są potrzebne. Redukuje to również liczbę widoków.

  Drugi wykres liniowy informuje o poniesionych szkodach w ostatich 5 miesiącach. W tym przypadku należało zmienić podejście względem statystki zarobków z ostatnich 5 miesięcy. Możemy się spodziewać, że w każdym miesiącu wystąpi chociaż jedna rezerwacja, więc wystarczyło z tabeli Reservations pogrupowane dane z ostatnich 5 miesięcy. Szkody nie muszą występować co miesiąc. Dlatego zastosowano inne podejście z zastosowaniem tabeli pomocniczej Miesiące.
  <br> <br> Tabela zawiera wszystkie miesiące od 2014 do 2034 w formacie np. '2024-06-01'
- `Miesiące` 
  - `Miesiąc` - typu date


  ```mysql
  CREATE VIEW SumOfDamages AS
    SELECT EXTRACT(YEAR FROM m.Miesiąc)  AS Rok,
         EXTRACT(MONTH FROM m.Miesiąc)   AS Miesiąc,
         COALESCE(SUM(d.Price), 0)       AS Szkody
    FROM Miesiące m 
        LEFT JOIN Damages d ON date_format(d.Date, '%Y-%m-01') = m.Miesiąc
      WHERE EXTRACT(YEAR FROM m.Miesiąc) <= YEAR(NOW())
        AND EXTRACT(MONTH FROM m.Miesiąc) <= MONTH(NOW())
      GROUP BY EXTRACT(YEAR FROM m.Miesiąc), m.Miesiąc
      ORDER BY EXTRACT(YEAR FROM m.Miesiąc) DESC,
               EXTRACT(MONTH FROM m.Miesiąc) DESC
      LIMIT 5;
  ```
---

<div style="page-break-after: always;"></div>

  ## Pokoje
  To prosty ekran do przeglądania dostępnych pokoi oraz dodawania nowych. Przycisk plus ukazuje okno do dodawania nowych pokoi, a odświeżenie ładuje ponownie wyniki, by pokazywały się dodane na nowo pokoje. 

  <img src="zrzuty_ekranu/pokoje.png" alt="Pokoje" width="500"/>

  Tabela pokoi to zwykły SELECT z tabeli Rooms. Dodawawanie pokoju obsługuje procedura:
  ```mysql
  CREATE PROCEDURE AddRoom(IN p_NumberOfPlaces int, IN p_Price int)
    BEGIN
        INSERT INTO Rooms (NumberOfPlaces, Price)
        VALUES (p_NumberOfPlaces, p_Price);
    END;
  ```
---

<div style="page-break-after: always;"></div>

  ## Klienci
  Ekran z listą wszystkich klientów, którzy złożyli rezerwację w hotelu.

  <img src="zrzuty_ekranu/klienci.png" alt="Klienci" width="500"/>

  Można wyszukiwać ich po nazwisku, po kilku filtrach oraz sortując dowolną kolumnę rosnąco lub malejąco.

  <img src="zrzuty_ekranu/klienci_z_filtrami.png" alt="Klienci" width="500"/>

<div style="page-break-after: always;"></div>

  ```mysql
  CREATE VIEW CustomerFullInfo AS
    SELECT p.PersonID AS PersonID,
           p.Name     AS Name,
           p.Surname  AS Surname,
           p.StreetAddress AS Address,
           c.CountryName   AS Country,
           ci.CityName     AS City,
           p.PhoneNumber   AS PhoneNumber,
           p.Email         AS Email,
           (CASE WHEN rc.PersonID is not null
               THEN 'True' ELSE 'False' END)      AS IsRegular,
           (CASE WHEN bc.PersonID is not null 
               THEN 'True' ELSE 'False' END)      AS IsBanned
    FROM Persons p join Countries c on p.CountryID = c.CountryID 
                   join Cities ci on p.CityID = ci.CityID
                   left join RegularCustomers rc on p.PersonID = rc.PersonID
                   left join BannedCustomers bc on p.PersonID = bc.PersonID;
  ```
  

Klienci nieproszeni to tacy, którzy wykonali więcej niż 2 szkody lub na ponad 1000 złotych. Na tych klientów nie można złożyć rezerwacji.
```mysql
CREATE VIEW BannedCustomers AS
  SELECT P.PersonID        AS PersonID,
         P.Name            AS Name,
         P.Surname         AS Surname,
         COUNT(D.DamageID) AS DamageCount,
         SUM(D.Price)      AS TotalDamage
  FROM Persons P join Reservations R on P.PersonID = R.PersonID
                 join Damages D on R.ReservationID = D.ReservationID
    GROUP BY P.PersonID, P.Name, P.Surname
    HAVING (COUNT(D.DamageID) > 2) or (SUM(D.Price) > 1000);
```
Stali klienci złożyli więcej niż 4 rezerwacje lub wydali w hotelu więcej niż 3000 złotych. Naliczany jest dla nich rabat procentowy na kolejne rezerwacje.

```mysql
CREATE VIEW RegularCustomers AS
  SELECT P.PersonID                         AS PersonID,
         P.Name                             AS Name,
         P.Surname                          AS Surname,
         COUNT(R.ReservationID)             AS ReservationCount,
         SUM(R.Price)                       AS TotalReservationPrice,
         (4 + floor((SUM(R.Price) / 1000))) AS Discount
  FROM Persons P JOIN Reservations R on P.PersonID = R.PersonID
    GROUP BY P.PersonID, P.Name, P.Surname
    HAVING (COUNT(R.ReservationID) > 4) OR (SUM(R.Price) > 3000);
```
---
## Rezerwacje
  Widok na wszystkie hotelowe rezerwacje.  

  <img src="zrzuty_ekranu/rezerwacje.png" alt="Rezerwacje" width="500"/> 

  Można wyszukiwać je po nazwisku klienta, po dacie pobytu oraz sortować każdą kolumnę. 
  
  <img src="zrzuty_ekranu/rezerwacje_z_filtrami.png" alt="Rezerwacje" width="500"/>

<div style="page-break-after: always;"></div>

```mysql
CREATE VIEW ReservationDetails AS
SELECT R.ReservationID                                 AS ReservationID,
       P.Name                                          AS PersonName,
       P.Surname                                       AS PersonSurname,
       Ro.RoomID                                       AS RoomID,
       Ro.NumberOfPlaces                               AS NumberOfPlaces,
       Ro.Price                                        AS RoomPrice,
       R.StartDate                                     AS StartDate,
       R.EndDate                                       AS EndDate,
       (to_days(R.EndDate) - to_days(R.StartDate))     AS NumberOfDays,
       R.Price                                         AS ReservationPrice,
       R.Discount                                      AS Discount
FROM Reservations R JOIN Persons P on R.PersonID = P.PersonID
                    JOIN Rooms Ro on R.RoomID = Ro.RoomID;
```

Filtry nakładano poleceniem WHERE na tym widoku.

---
## Szkody
  <img src="zrzuty_ekranu/szkody.png" alt="Szkody" width="500"/>

Panel szkód działa podobnie do panelu Pokoje. Również można dodawać nowe szkody, a następnie odświeżać listę. Jednak tym razem należało wprowadzić kontrolę wprowadzanych danych.

<div style="page-break-after: always;"></div>

```mysql
CREATE PROCEDURE AddDamage(IN p_ReservationID int, IN p_Date date,
                           IN p_Price int)
BEGIN
    DECLARE v_StartDate DATE;
    DECLARE v_EndDate DATE;

    SELECT StartDate, EndDate INTO v_StartDate, v_EndDate
    FROM Reservations
        WHERE ReservationID = p_ReservationID;

    IF v_StartDate IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Nie istnieje rezerwacja o podanym ReservationID';
    END IF;

    IF p_Date < v_StartDate OR p_Date > v_EndDate THEN
        SIGNAL SQLSTATE '45001'
        SET MESSAGE_TEXT = 'Data szkody musi być pomiędzy StartDate i EndDate rezerwacji';
    END IF;

    INSERT INTO Damages (ReservationID, Date, Price)
    VALUES (p_ReservationID, p_Date, p_Price);

END;
```

---

# Widok klienta
Widok klienta zawiera infomacje interesujące konkretnego klienta. Funckjonalności z wyjątkiem logowania zostaną omówione na przykładzie zalogowanego użytkownika.

<img src="zrzuty_ekranu/k_log.png" width="500">

<div style="page-break-after: always;"></div>

## Logowanie do systemu
Logowanie do systemu zostało uproszczone i odbywa się jedynie poprzez podanie prawidłowego adresu e-mail (czyli takiego, który występuje w bazie danych). Możemy również podać e-mail, którego nie ma w bazie- wtedy jesteśmy traktowani jako niezalogowani.

### Bez logowania
Jeżeli podamy e-mail nieistniejący w bazie, utracimy dostęp do pewnych funckjonalności. 
Jednak w zakłdace `Dane` dostaniemy możliwość utworzenia konta.

<img src="zrzuty_ekranu/niezalogowany.png" width="500">


<div style="page-break-after: always;"></div>

Jest to realizowane przez funckję `AddCustomer`:
```sql
create procedure AddCustomer(IN CustomerName varchar(20), IN CustomerSurname varchar(20),
                                                   IN CustomerStreetAddress varchar(50),
                                                   IN CustomerCityName varchar(20),
                                                   IN CustomerCountryName varchar(20),
                                                   IN CustomerPhoneNumber varchar(10), 
                                                   IN CustomerEmail varchar(30))
BEGIN
    DECLARE CityID INT;
    DECLARE CountryID INT;

    IF NOT EXISTS (SELECT 1 FROM Countries WHERE CountryName = CustomerCountryName) THEN
        INSERT INTO Countries (CountryName) VALUES (CustomerCountryName);
    END IF;

    SELECT CountryID INTO CountryID FROM Countries WHERE CountryName = CustomerCountryName;

    IF NOT EXISTS (SELECT 1 FROM Cities WHERE CityName = CustomerCityName) THEN
        INSERT INTO Cities (CityName) VALUES (CustomerCityName);
    END IF;

    SELECT CityID INTO CityID FROM Cities WHERE CityName = CustomerCityName;

    INSERT INTO Persons (Name, Surname, StreetAddress, CityID, CountryID, PhoneNumber, Email)
    VALUES (CustomerName, CustomerSurname, CustomerStreetAddress, CityID, CountryID, 
            CustomerPhoneNumber, CustomerEmail);
END;
```

<div style="page-break-after: always;"></div>

### Poprawny e-mail
Zalogujmy się jako Tomasz Kowalczyk (tomasz.kowalczyk@example.com). Po zalogowaniu widzimy nasze dane.

<img src="zrzuty_ekranu/k_dane_z_bazy.png" width="500">

---

## Historia rezerwacji

Historia rezerwacji pozwala klientowi przeglądać swoje rezerwacje oraz zarządzać ich statusem- płacić i odwoływać.

<img src="zrzuty_ekranu/historia_rezerwacji.png" width="500"/>

System kontroluje, czy Klient aby na pewno podaje ID swojej Rezerwacji. Kontroluje również czy Klient nie próbuje zapłacić za rezerwację odwołaną- taka transakcja jest przerywana.

Płatność obsługuje następująca procedura:


```sql
CREATE PRODEDURE PayForReservation(IN v_reservationId int)
BEGIN
    DECLARE currentStatus INT;

    SELECT StatusID INTO currentStatus
    FROM Reservations
    WHERE ReservationID = v_reservationId;

    IF currentStatus = 4 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Nie można zapłacić za odwołaną rezerwację';
    ELSE
        CALL UpdateReservationStatus(v_reservationId, 3);
    END IF;
END;
```

Odwoływanie:

```sql
CREATE PROCEDURE CancelReservation(IN v_reservationId int)
BEGIN
    CALL UpdateReservationStatus(v_reservationId, 4);
END;
```

Funkcja aktualizująca status podanej rezerwacji uzupełnia również tabelę Logs:

```sql
CREATE PROCEDURE UpdateReservationStatus(IN v_reservationId int, IN newStatusId int)
BEGIN
    UPDATE Reservations
    SET StatusID = newStatusId
    WHERE ReservationID = v_reservationId;

    INSERT INTO Logs(ReservationID, StatusID, DateTime)
    VALUES (v_reservationId, newStatusId, NOW());
END;

```

---

<div style="page-break-after: always;"></div>

## Nowa rezerwacja
Możemy ustawić minimalną i maksymlną kwotę za noc jak i liczbę miejsc, która nas interesuje.

Osoba niezalogowana może jedynie przeglądać wolne pokoje, nie może jednak uworzyć rezerwacji.

Nie możemy dodać rezerwacji przed wybraniem daty początkowej i końcowej.

<img src="zrzuty_ekranu/k_choose_date.png" width="500"/>

Po wypełnieniu dat, możemy kliknąć przycisk `Szukaj`.

Otrzymujemy dostępne pokoje zgodne z podanymi kryteriami. Korzystamy tutaj z procedury `AvaiableRooms`:
```sql
create procedure AvailableRooms(IN StartDate date, IN EndDate date, IN NumberOfPlaces int,
                                                      IN MinPrice int, IN MaxPrice int)
BEGIN
    SELECT r.RoomID, r.NumberOfPlaces, r.Price
    FROM Rooms r
    WHERE r.NumberOfPlaces = NumberOfPlaces
        AND r.Price BETWEEN MinPrice AND MaxPrice
        AND IsRoomAvailable(r.RoomID, StartDate, EndDate) = 1;
END;

```
Ta procedura korzysta natomiast z procedury `IsRoomAvailable`, która szuka pokoi, które nie posiadają rezerwacji w danym terminie (mowa o nieodwołanych rezerwacjach).
<div style="page-break-after: always;"></div>

```sql
create function IsRoomAvailable(RoomIDParam int, StartDateParam date, EndDateParam date) 
    returns int
BEGIN
    DECLARE RoomCount INT;
            
SELECT COUNT(*) INTO RoomCount
FROM Reservations
WHERE RoomID = RoomIDParam
  AND StartDate <= EndDateParam
  AND EndDate >= StartDateParam
  AND StatusID <> 4;

IF RoomCount > 0 THEN
        RETURN 0;
ELSE
        RETURN 1;
END IF;
END;
```

Możemy teraz dodać nową rezerwację. Zostaniemy poproszeni o ID interesującego nas pokokju:

<img src="zrzuty_ekranu/k_choose_num.png" width="500"/>

Wybierzmy pokój 25. Po zatwierdzeniu, dostajemy komunikat o dodanej rezerwacji.

<img src="zrzuty_ekranu/k_res_suc.png" width="500">

A na koniec widok dostępnych pokoi w danym terminie się aktualizuje:

<img src="zrzuty_ekranu/k_res_suc_po.png" width="500"/>

Możemy również zobaczyć, że nowa rezerwacja pojawiła się w bazie danych:

<img src="zrzuty_ekranu/k_res.png" width="500"/>

<div style="page-break-after: always;"></div>

Jeżeli ktoś, jest na liście klientów nieproszonych, nie ma możliwości złożenia rezerwacji.

<img src="zrzuty_ekranu/nieproszony.png" width="400"/>
---

## Szkody
Logując się przykładowo jako Andrzej Lewandowski (andrzej.lewandowski@example.com), gdy wejdziemy w zakładki `Szkody` zobaczymy wszystkie szkody spowodowane przez Andrzeja. 

Korzystamy tutaj procedury `GetDamagesForPerson`:
```sql
create procedure GetDamagesForPerson(IN personID int)
BEGIN
    SELECT d.*
    FROM Damages d
             JOIN Reservations r ON d.reservationID = r.reservationID
    WHERE r.personID = personID;
END;
```

Dla osób nie mających szkód, pokazana zostanie pusta tabela:
<img src="zrzuty_ekranu/k_no_dam.png" width="500"/>