# PHP mínimo declarado pelos projetos Composer da revisão analisada.
FROM php:7.4-cli-bullseye
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
      libfreetype6-dev libjpeg62-turbo-dev libpng-dev libzip-dev libldap2-dev \
      libicu-dev libonig-dev libcurl4-openssl-dev unzip git mariadb-client; \
    docker-php-ext-configure gd --with-freetype --with-jpeg; \
    docker-php-ext-configure ldap --with-libdir=lib/$(uname -m)-linux-gnu/; \
    docker-php-ext-install -j "$(nproc)" gd intl pdo_mysql zip ldap mbstring curl; \
    echo 'memory_limit=1G' > /usr/local/etc/php/conf.d/memory.ini; \
    rm -rf /var/lib/apt/lists/*
COPY --from=composer:2.8 /usr/bin/composer /usr/bin/composer
