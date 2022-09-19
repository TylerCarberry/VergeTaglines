# [@VergeTaglines](https://twitter.com/VergeTaglines)  [![Twitter Follow](https://img.shields.io/twitter/follow/vergetaglines.svg?style=social&label=Follow)](https://twitter.com/vergetaglines)


A Twitter bot that tweets the current header and tagline of [theverge.com](https://theverge.com)

### How it works
- The program is hosted on [Google Cloud Run](https://cloud.google.com/run) and is triggered every 10 minutes using [Cloud Scheduler](https://cloud.google.com/scheduler)
- The homepage of [theverge.com](https://theverge.com) is scraped using [JSoup](https://jsoup.org/)
- The header and tagline are saved to a MySQL database
- A screenshot is generated using [ApiFlash](https://apiflash.com)
- If the header or tagline have changed, a tweet is sent using [Twitter4j](http://twitter4j.org/en/)

  

<img width="1024" alt="screen shot 2018-08-12 at 7 00 45 pm" src="https://user-images.githubusercontent.com/6628497/44009627-1258a168-9e62-11e8-839a-aad6553966aa.png">
