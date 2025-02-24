# Потокобезопасный WebCrawler

## Описание
Этот репозиторий содержит **потокобезопасный WebCrawler**, который рекурсивно обходит веб-сайты.

## Конструктор
```java
public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost)
```
- `downloader` – позволяет загружать страницы и извлекать из них ссылки.
- `downloaders` – максимальное число одновременно загружаемых страниц.
- `extractors` – максимальное число страниц, из которых извлекаются ссылки.
- `perHost` – максимальное число страниц, одновременно загружаемых с одного хоста.

## Реализация интерфейса
Класс `WebCrawler` реализует интерфейс `Crawler`:
```java
public interface Crawler extends AutoCloseable {
    List<String> download(String url, int depth) throws IOException;
    void close();
}
```

## Функциональность
- Метод `download` рекурсивно обходит страницы, начиная с указанного URL, до заданной глубины и возвращает список загруженных страниц и файлов.
- Если глубина `1`, загружается только указанная страница.
- Если глубина `2`, загружается указанная страница и страницы/файлы, на которые она ссылается, и так далее.
- Метод может выполняться параллельно в нескольких потоках.

## Параллельное выполнение
- Загрузка и обработка страниц (извлечение ссылок) выполняется максимально параллельно с учетом ограничений:
  - Количество одновременно загружаемых страниц.
  - Количество страниц, из которых извлекаются ссылки.
  - Количество страниц, загружаемых с одного хоста.
- Метод `close` завершает все вспомогательные потоки.

## Использование Downloader
Для загрузки страниц используется `Downloader`, передаваемый в конструктор.
```java
public interface Downloader {
    public Document download(final String url) throws IOException;
}
```

## Извлечение ссылок
Документ позволяет получить ссылки из загруженной страницы:
```java
public interface Document {
    List<String> extractLinks() throws IOException;
}
```
- Ссылки, возвращаемые документом, являются абсолютными и имеют схему `http` или `https`.

## Использование из командной строки
Метод `main` позволяет запустить обход из командной строки:
```sh
WebCrawler url [depth [downloads [extractors [perHost]]]]
```
