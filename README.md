# sirnik.Dev

[![Coverage](.github/badges/jacoco.svg)](https://nikgil.github.io/Blog/)

A personal developer blog built with Spring Boot, FreeMarker, Bulma, and htmx.

## Run locally

### Prerequisites

- Java 21
- Git

The Maven Wrapper is included, so a separate Maven installation is not
required.

Clone the repository and start the application:

```sh
git clone https://github.com/nikgil/Blog.git
cd Blog
./mvnw spring-boot:run
```

Open `http://localhost:8080`. Local development uses an in-memory H2 database,
runs the Flyway migrations, and generates development posts when the application
starts. The data is discarded after the application process stops.

Run the automated checks with:

```sh
./mvnw test
./mvnw clean verify
```

## Deploy to a production server

This example assumes a Linux server with `systemd`, PostgreSQL, Nginx, Java 21,
SSH access, and a domain whose DNS records point to the server. Replace
`example.com`, usernames, passwords, and paths with values for your server.

### 1. Build the application

Build and verify the executable Spring Boot JAR locally:

```sh
./mvnw clean verify
```

The resulting application is `target/blog-0.0.1-SNAPSHOT.jar`. Copy it to the
server:

```sh
scp target/blog-0.0.1-SNAPSHOT.jar deploy@example.com:/tmp/blog.jar
```

### 2. Create the PostgreSQL database

Create a database and a dedicated database user. For example, from the server:

```sh
sudo -u postgres createuser --pwprompt blog
sudo -u postgres createdb --owner=blog blog
```

The production profile runs Flyway automatically when the application starts,
so this user must be allowed to create and alter objects in the `blog` database.

### 3. Install the JAR and configuration

Create a system user and application directory, then install the uploaded JAR:

```sh
sudo useradd --system --home /opt/sirnik-blog --shell /usr/sbin/nologin sirnik-blog
sudo install -d -o sirnik-blog -g sirnik-blog /opt/sirnik-blog
sudo install -o sirnik-blog -g sirnik-blog /tmp/blog.jar /opt/sirnik-blog/blog.jar
```

Create `/etc/sirnik-blog.env` with the production profile and database
credentials:

```text
SPRING_PROFILES_ACTIVE=prod
BLOG_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/blog
BLOG_DATABASE_USERNAME=blog
BLOG_DATABASE_PASSWORD=replace-with-a-strong-password
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8080
SERVER_FORWARD_HEADERS_STRATEGY=framework
```

Restrict the file so only root can read the credentials:

```sh
sudo chown root:root /etc/sirnik-blog.env
sudo chmod 600 /etc/sirnik-blog.env
```

The `prod` profile reads the three `BLOG_DATABASE_*` values from the environment.
Binding the application to `127.0.0.1` prevents direct public access to port
8080; Nginx will be the public entry point.

### 4. Run the application with systemd

Create `/etc/systemd/system/sirnik-blog.service`:

```ini
[Unit]
Description=sirnik.Dev blog
After=network.target postgresql.service

[Service]
Type=exec
User=sirnik-blog
Group=sirnik-blog
WorkingDirectory=/opt/sirnik-blog
EnvironmentFile=/etc/sirnik-blog.env
ExecStart=/usr/bin/java -jar /opt/sirnik-blog/blog.jar
Restart=on-failure
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

Enable and start it:

```sh
sudo systemctl daemon-reload
sudo systemctl enable --now sirnik-blog
sudo systemctl status sirnik-blog
```

Application logs are available with:

```sh
sudo journalctl -u sirnik-blog -f
```

Spring Boot documents both executable JARs and `systemd` services in its
[deployment guide](https://docs.spring.io/spring-boot/how-to/deployment/installing.html).

### 5. Connect the domain with Nginx and HTTPS

Create an Nginx server block for the domain. A minimal HTTP configuration is:

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name example.com www.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable the server block using the layout provided by the server's Nginx
package, run `sudo nginx -t`, and reload Nginx. The relevant directives are
described by the
[Nginx proxy module](https://nginx.org/en/docs/http/ngx_http_proxy_module.html).

After the domain resolves to the server, obtain a TLS certificate with an ACME
client such as Certbot and configure Nginx to redirect HTTP to HTTPS. Expose only
SSH, HTTP, and HTTPS through the firewall; keep PostgreSQL and application port
8080 private.

### 6. Deploy an update

Build and verify a new JAR, upload it, replace the installed artifact, and
restart the service:

```sh
./mvnw clean verify
scp target/blog-0.0.1-SNAPSHOT.jar deploy@example.com:/tmp/blog.jar
ssh deploy@example.com
sudo install -o sirnik-blog -g sirnik-blog /tmp/blog.jar /opt/sirnik-blog/blog.jar
sudo systemctl restart sirnik-blog
sudo systemctl status sirnik-blog
```

Back up PostgreSQL before deployments that introduce new Flyway migrations.

## TODO

- [ ] Menu bar to the right of the header
- [ ] Make search work
- [ ] Make tag search work
- [ ] Add a left-hand bar with months and years
