# Grash CMMS API

This project aims to help manage assets, schedule maintenance and track work orders. This is the REST backend of the web
application developed with React. The frontend developed with React can be
found [here](https://github.com/Grashjs/frontend). We also have a [React Native
mobile application](https://github.com/Grashjs/mobile). The link to the live website can be found [here](https://grash-cmms.com).  
You can see more explanations in [Purpose.pdf](Purpose.pdf). We would be very happy to have new contributors join us.
And please star the repo.

**Screenshot**:
![](https://i.ibb.co/7tGYCtv/Screenshot-502.png)

## Configuration

You should create a database, then go to `src/main/resources/application-dev.yml`, set the url, username and password.
The following is optional. You can set these environment variables.

- `SMTP_USER` after [creating an app password with Google](https://support.google.com/accounts/answer/185833?hl=en)
- `SMTP_PWD` after [creating an app password with Google](https://support.google.com/accounts/answer/185833?hl=en)
- `GCP_JSON` after creating a service account following the section **Create a service account** of
  this [tutorial](https://medium.com/@raviyasas/spring-boot-file-upload-with-google-cloud-storage-5445ed91f5bc)

## Getting help

If you have questions, concerns, bug reports, etc, please file an issue in this repository's Issue Tracker or send an
email at ibracool99@gmail.com.

## Getting involved

You can contribute in different ways. Sending feedback on features, fixing certain bugs, implementing new features, etc.
Here is the [trello dashboard](https://trello.com/invite/b/dHcnX2Y0/ATTI9f361dff4298643df8ef3a80a1413c42E4308099/grash).
Instructions on _how_ to contribute can be found in [CONTRIBUTING](CONTRIBUTING.md).


----

## Open source licensing info

1. [TERMS](TERMS.md)
2. [LICENSE](LICENSE)
