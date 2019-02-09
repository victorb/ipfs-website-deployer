variable "do_token" {}

variable "go_ipfs_version" {
  default = "v0.4.18"
}

provider "digitalocean" {
  token = "${var.do_token}"
}

resource "digitalocean_droplet" "web" {
  image    = "ubuntu-18-04-x64"
  name     = "ipfs-website-deploy-01"
  region   = "sfo2"
  size     = "s-1vcpu-1gb"
  ssh_keys = [14693619]
}

resource "null_resource" "app" {
  triggers = {
    web_ids         = "${join(",", digitalocean_droplet.web.*.id)}"
    go_ipfs_version = "${var.go_ipfs_version}"
  }

  connection {
    host = "${element(digitalocean_droplet.web.*.ipv4_address, count.index)}"
  }

  provisioner "remote-exec" {
    inline = [
      "fallocate -l 1G /swapfile || true",
      "chmod 600 /swapfile || true",
      "mkswap /swapfile || true",
      "swapon /swapfile || true",
    ]
  }

  provisioner "file" {
    source      = "services/ipfs.service"
    destination = "/etc/systemd/system/ipfs.service"
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir /app",
      "wget https://dist.ipfs.io/go-ipfs/${var.go_ipfs_version}/go-ipfs_${var.go_ipfs_version}_linux-amd64.tar.gz -O go-ipfs.tar.gz",
      "tar xfv go-ipfs.tar.gz",
      "cd go-ipfs && ./install.sh",
      "ipfs init || true",
      "ipfs config --json Experimental.Libp2pStreamMounting true",
      "systemctl daemon-reload",
      "systemctl start ipfs",
      "echo ipfs has started",
      "sleep 10",
      "ipfs swarm connect /ip4/10.0.0.1/tcp/9097/ipfs/QmQCzVF95r7U9hybH37w359zBgh75Nz6v9RFAeCegBBEuc",
      "ipfs p2p forward --allow-custom-protocol /libp2p-http /ip4/127.0.0.1/tcp/1234 /ipfs/QmQCzVF95r7U9hybH37w359zBgh75Nz6v9RFAeCegBBEuc",
      "curl -u 'u:p' localhost:1234/id",
    ]
  }

  provisioner "local-exec" {
    command     = "lein ring uberjar"
    working_dir = "../"
  }

  provisioner "local-exec" {
    command     = "cp target/ipfs-website-deployer-0.1.0-SNAPSHOT-standalone.jar infra/ipfs-website-deployer.jar"
    working_dir = "../"
  }

  provisioner "file" {
    source      = "ipfs-website-deployer.jar"
    destination = "/app/ipfs-website-deployer.jar"
  }

  provisioner "file" {
    source      = ".env"
    destination = "/app/.env"
  }

  provisioner "file" {
    source      = "private.key"
    destination = "/app/private.key"
  }

  provisioner "file" {
    source      = "services/ipfs-website-deployer.service"
    destination = "/etc/systemd/system/ipfs-website-deployer.service"
  }

  provisioner "remote-exec" {
    inline = [
      "apt update && apt install --yes default-jre",
      "systemctl daemon-reload",
      "systemctl restart ipfs-website-deployer",
      "echo ipfs-website-deployer is now running",
    ]
  }
}

resource "digitalocean_certificate" "cert" {
  name    = "website-deploy"
  type    = "lets_encrypt"
  domains = ["website-deploy.app"]
}

resource "digitalocean_domain" "website" {
  name       = "website-deploy.app"
  ip_address = "${digitalocean_loadbalancer.public.ip}"
}

resource "digitalocean_record" "www" {
  domain = "${digitalocean_domain.website.name}"
  type   = "A"
  name   = "@"
  value  = "${digitalocean_loadbalancer.public.ip}"
}

resource "digitalocean_loadbalancer" "public" {
  name                   = "loadbalancer-1"
  region                 = "sfo2"
  redirect_http_to_https = true

  forwarding_rule {
    entry_port     = 443
    entry_protocol = "https"
    certificate_id = "${digitalocean_certificate.cert.id}"

    target_port     = 3000
    target_protocol = "http"
  }

  healthcheck {
    port     = 3000
    protocol = "http"
    path     = "/metrics"
  }

  droplet_ids = ["${digitalocean_droplet.web.id}"]
}
