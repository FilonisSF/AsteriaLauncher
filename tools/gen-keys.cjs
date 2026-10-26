// Creates the PREMIUM signing key pair (run once):  node tools/gen-keys.cjs
//  - private key -> backend/data/ticket-private.pem  (stays on your server, never commit it)
//  - public key  -> mods/common-visuals/.../Ticket.java (compiled into the visuals mod)
const crypto = require('crypto'), fs = require('fs'), path = require('path')
const root = path.join(__dirname, '..'), priv = path.join(root, 'backend', 'data', 'ticket-private.pem')
const tj = path.join(root, 'mods', 'common-visuals', 'ru', 'safimine', 'asteria', 'visuals', 'Ticket.java')
if (fs.existsSync(priv) && !process.argv.includes('--force')) { console.log('Key already exists:', priv, '(use --force to replace it; old tickets stop working)') }
else {
  const { privateKey, publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' })
  fs.mkdirSync(path.dirname(priv), { recursive: true }); fs.writeFileSync(priv, privateKey.export({ type: 'pkcs8', format: 'pem' }))
  const pub = publicKey.export({ type: 'spki', format: 'der' }).toString('base64')
  fs.writeFileSync(tj, fs.readFileSync(tj, 'utf8').replace(/PUBLIC_KEY = "[^"]*"/, `PUBLIC_KEY = "${pub}"`))
  console.log('New key pair written. Public key embedded into Ticket.java')
}
