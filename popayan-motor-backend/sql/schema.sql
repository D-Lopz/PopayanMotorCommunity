-- Popayán Motor Community — esquema MySQL 8
-- Ejecutar completo en MySQL Workbench (o: mysql -u root -p < sql/schema.sql)

/*CREATE DATABASE IF NOT EXISTS popayan_motor
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE popayan_motor;

CREATE TABLE member (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone         VARCHAR(20)  NULL,
  join_date     DATETIME     NOT NULL,
  role          ENUM('MEMBER','ADMINISTRATOR') NOT NULL DEFAULT 'MEMBER',
  CONSTRAINT uq_member_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE vehicle (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  member_id   INT          NOT NULL,
  brand       VARCHAR(50)  NOT NULL,
  model       VARCHAR(50)  NOT NULL,
  year        SMALLINT     NOT NULL,
  type        ENUM('Modificado','Deportivo','Clásico','Exclusivo','Sonido','Off-road','Tuning','Otro') NOT NULL,
  plate       VARCHAR(10)  NOT NULL,
  description TEXT         NULL,
  CONSTRAINT uq_vehicle_plate UNIQUE (plate),
  CONSTRAINT ck_vehicle_year CHECK (year >= 1900),
  CONSTRAINT fk_vehicle_member FOREIGN KEY (member_id)
    REFERENCES member (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE event (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(150) NOT NULL,
  description  TEXT         NULL,
  date         DATE         NOT NULL,
  time         TIME         NOT NULL,
  location     VARCHAR(200) NOT NULL,
  max_capacity INT          NOT NULL,
  category     ENUM('Meet','Exhibición','Ruta','Competencia','Evento de sonido','Reunión','Otro') NOT NULL,
  CONSTRAINT ck_event_capacity CHECK (max_capacity > 0)
) ENGINE=InnoDB;

CREATE TABLE registration (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  member_id         INT      NOT NULL,
  event_id          INT      NOT NULL,
  registration_date DATETIME NOT NULL,
  CONSTRAINT uq_registration_member_event UNIQUE (member_id, event_id),
  CONSTRAINT fk_registration_member FOREIGN KEY (member_id)
    REFERENCES member (id) ON DELETE CASCADE,
  CONSTRAINT fk_registration_event FOREIGN KEY (event_id)
    REFERENCES event (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_event_date ON event (date);
CREATE INDEX idx_event_category ON event (category);
CREATE INDEX idx_vehicle_member ON vehicle (member_id);
