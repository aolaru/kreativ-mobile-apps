#!/bin/sh
# Reissue with the SAME protected key. Never put the private key or code in Git.
set -eu
umask 077
if [ "$#" -ne 2 ]; then
    echo "Usage: sh scripts/issue-review-code.sh /absolute/private.pem /absolute/output-code.txt" >&2
    exit 1
fi
review_private=$1
review_output=$2
case "$review_private:$review_output" in /*:/*) ;; *) echo "Use absolute paths." >&2; exit 1 ;; esac
if [ -e "$review_output" ]; then echo "Output already exists; refusing to overwrite." >&2; exit 1; fi
review_work=$(mktemp -d)
trap 'rm -f "$review_work/payload" "$review_work/signature" "$review_work/public.der"; rmdir "$review_work"' EXIT
printf '%s' 'paperbuild-review-v1:com.kreativ.paperbuild:pro' > "$review_work/payload"
openssl ec -in "$review_private" -pubout -param_enc named_curve -outform DER -out "$review_work/public.der"
review_public=$(openssl base64 -A -in "$review_work/public.der")
if [ "$review_public" != 'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEmTO/zahpqEYaNHu91cE+LhHOXF8u02moz/acxtvy18jjA3YiHk952idjGGuGU2cHPHovyXQ6LP4Vdpj4xi54Lw==' ]; then
    echo "Key does not match this app's review verification key." >&2; exit 1
fi
openssl dgst -sha256 -sign "$review_private" -out "$review_work/signature" "$review_work/payload"
printf 'PBR1.' > "$review_output"
openssl base64 -A -in "$review_work/signature" | tr '+/' '-_' | tr -d '=' >> "$review_output"
printf '\n' >> "$review_output"
echo "Review code saved privately. Do not publish it or commit it to Git."
