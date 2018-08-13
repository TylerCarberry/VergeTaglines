# [@VergeTaglines](https://twitter.com/VergeTaglines)  [![Twitter Follow](https://img.shields.io/twitter/follow/vergetaglines.svg?style=social&label=Follow)](https://twitter.com/vergetaglines)


A Twitter bot that automatically tweets the current header of [theverge.com](https://theverge.com)

### How it works
Every 10 minutes an AWS Cloudwatch timer triggers an AWS Lambda function. It then scrapes the current homepage of [theverge.com](https://theverge.com) and stores it into the database. If the tagline has changed, it tweets out the current homepage. The screenshots are generated using [Screenshot Layer](https://screenshotlayer.com). 

  

<img width="1024" alt="screen shot 2018-08-12 at 7 00 45 pm" src="https://user-images.githubusercontent.com/6628497/44009627-1258a168-9e62-11e8-839a-aad6553966aa.png">
