// Unicode mappings reused from Kreativ Font Translator. No WordPress dependency.
    const baseLower  = Array.from("abcdefghijklmnopqrstuvwxyz");
    const baseUpper  = Array.from("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    const baseDigits = Array.from("0123456789");

    const toChars = str => Array.from(str);

    // --------------------------------------------------
    // Unicode maps (Unicode "fonts")
    // --------------------------------------------------
    const maps = {
        bold: {
            lower: toChars("𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳"),
            upper: toChars("𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙"),
            digits: toChars("𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗")
        },
        italic: {
            lower: toChars("𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧"),
            upper: toChars("𝐴𝐵𝐶𝐷𝐸𝐹𝐺𝐻𝐼𝐽𝐾𝐿𝑀𝑁𝑂𝑃𝑄𝑅𝑆𝑇𝑈𝑉𝑊𝑋𝑌𝑍"),
            digits: baseDigits
        },
        bold_italic: {
            lower: toChars("𝒂𝒃𝒄𝒅𝒆𝒇𝒈𝒉𝒊𝒋𝒌𝒍𝒎𝒏𝒐𝒑𝒒𝒓𝒔𝒕𝒖𝒗𝒘𝒙𝒚𝒛"),
            upper: toChars("𝑨𝑩𝑪𝑫𝑬𝑭𝑮𝑯𝑰𝑱𝑲𝑳𝑴𝑵𝑶𝑷𝑸𝑹𝑺𝑻𝑼𝑽𝑾𝑿𝒀𝒁"),
            digits: baseDigits
        },
        sans: {
            lower: toChars("𝖺𝖻𝖼𝖽𝖾𝖿𝗀𝗁𝗂𝗃𝗄𝗅𝗆𝗇𝗈𝗉𝗊𝗋𝗌𝗍𝗎𝗏𝗐𝗑𝗒𝗓"),
            upper: toChars("𝖠𝖡𝖢𝖣𝖤𝖥𝖦𝖧𝖨𝖩𝖪𝖫𝖬𝖭𝖮𝖯𝖰𝖱𝖲𝖳𝖴𝖵𝖶𝖷𝖸𝖹"),
            digits: toChars("𝟢𝟣𝟤𝟥𝟦𝟧𝟨𝟩𝟪𝟫")
        },
        sans_bold: {
            lower: toChars("𝗮𝗯𝗰𝗱𝗲𝗳𝗴𝗵𝗶𝗷𝗸𝗹𝗺𝗻𝗼𝗽𝗾𝗿𝘀𝘁𝘂𝘃𝘄𝘅𝘆𝘇"),
            upper: toChars("𝗔𝗕𝗖𝗗𝗘𝗙𝗚𝗛𝗜𝗝𝗞𝗟𝗠𝗡𝗢𝗣𝗤𝗥𝗦𝗧𝗨𝗩𝗪𝗫𝗬𝗭"),
            digits: toChars("𝟬𝟭𝟮𝟯𝟰𝟱𝟲𝟳𝟴𝟵")
        },
        script: {
            lower: toChars("𝓪𝓫𝓬𝓭𝓮𝓯𝓰𝓱𝓲𝓳𝓴𝓵𝓶𝓷𝓸𝓹𝓺𝓻𝓼𝓽𝓾𝓿𝔀𝔁𝔂𝔃"),
            upper: toChars("𝓐𝓑𝓒𝓓𝓔𝓕𝓖𝓗𝓘𝓙𝓚𝓛𝓜𝓝𝓞𝓟𝓠𝓡𝓢𝓣𝓤𝓥𝓦𝓧𝓨𝓩"),
            digits: baseDigits
        },
        fraktur: {
            lower: toChars("𝔞𝔟𝔠𝔡𝔢𝔣𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷"),
            upper: toChars("𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ"),
            digits: baseDigits
        },
        double: {
            lower: toChars("𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫"),
            upper: toChars("𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ"),
            digits: toChars("𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡")
        },
        mono: {
            lower: toChars("𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣"),
            upper: toChars("𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉"),
            digits: toChars("𝟶𝟷𝟸𝟹𝟺𝟻𝟼𝟽𝟾𝟿")
        },

        // Extra mapped styles
        old_english: {
            lower: toChars("𝔞𝔟𝔠𝔡𝔢𝔣𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷"),
            upper: toChars("𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ"),
            digits: baseDigits
        },
        bubble: {
            lower: toChars("ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ"),
            upper: toChars("ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ"),
            digits: toChars("⓪①②③④⑤⑥⑦⑧⑨")
        },
        small_caps: {
            lower: toChars("ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ"),
            upper: toChars("ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ"),
            digits: baseDigits
        },
        tiny: {
            lower: toChars("ᵃᵇᶜᵈᵉᶠᵍʰᶦʲᵏˡᵐⁿᵒᵖᑫʳˢᵗᵘᵛʷˣʸᶻ"),
            upper: baseUpper,
            digits: toChars("⁰¹²³⁴⁵⁶⁷⁸⁹")
        },
        emoji: {
            lower: [
                "🇦","🇧","🇨","🇩","🇪","🇫","🇬","🇭","🇮","🇯","🇰","🇱",
                "🇲","🇳","🇴","🇵","🇶","🇷","🇸","🇹","🇺","🇻","🇼","🇽","🇾","🇿"
            ],
            upper: [
                "🇦","🇧","🇨","🇩","🇪","🇫","🇬","🇭","🇮","🇯","🇰","🇱",
                "🇲","🇳","🇴","🇵","🇶","🇷","🇸","🇹","🇺","🇻","🇼","🇽","🇾","🇿"
            ],
            digits: ["0️⃣","1️⃣","2️⃣","3️⃣","4️⃣","5️⃣","6️⃣","7️⃣","8️⃣","9️⃣"]
        }
    };

    // --------------------------------------------------
    // Zalgo & advanced transform styles
    // --------------------------------------------------
    const zalgo_up = [
        "\u030D","\u030E","\u0304","\u0305","\u033F","\u0311","\u0306","\u0310",
        "\u0352","\u0357","\u0351","\u0307","\u0308","\u030A","\u0342","\u0343",
        "\u0344","\u034A","\u034B","\u034C"
    ];
    const zalgo_mid = [
        "\u0334","\u0335","\u0336","\u0337","\u0338","\u0340","\u0341","\u0358"
    ];
    const zalgo_down = [
        "\u0316","\u0317","\u0318","\u0319","\u031C","\u031D","\u031E","\u031F",
        "\u0320","\u0324","\u0325","\u0326","\u0329","\u032A","\u032B","\u032C",
        "\u032D","\u032E","\u032F","\u0330","\u0331","\u0332","\u0333"
    ];

    function addZalgo(text, level = 1) {
        return Array.from(text).map(char => {
            let out = char;
            for (let i = 0; i < level; i++) {
                out += zalgo_up[Math.floor(Math.random() * zalgo_up.length)];
                if (level >= 2)
                    out += zalgo_mid[Math.floor(Math.random() * zalgo_mid.length)];
                if (level >= 3)
                    out += zalgo_down[Math.floor(Math.random() * zalgo_down.length)];
            }
            return out;
        }).join('');
    }

    maps.zalgolight = { transform: t => addZalgo(t, 1) };
    maps.zalgomedium = { transform: t => addZalgo(t, 2) };
    maps.zalgomax    = { transform: t => addZalgo(t, 4) };

    maps.kawaii = {
        transform: text => `☆ ${Array.from(text).join(" ✧ ")} ☆`
    };

    // Vaporwave fullwidth (ＡＥＳＴＨＥＴＩＣ)
    maps.vaporwave = {
        transform: text =>
            Array.from(text).map(c => {
                const code = c.charCodeAt(0);
                if (code >= 33 && code <= 126) {
                    return String.fromCharCode(code + 65248);
                }
                return c;
            }).join("")
    };

    // Upside-down letters
    maps.upsidedown = {
        transform: text => {
            const flipMap = {
                a:"ɐ", b:"q", c:"ɔ", d:"p", e:"ǝ", f:"ɟ", g:"ƃ", h:"ɥ", i:"ᴉ", j:"ɾ",
                k:"ʞ", l:"ʃ", m:"ɯ", n:"u", o:"o", p:"d", q:"b", r:"ɹ", s:"s", t:"ʇ",
                u:"n", v:"ʌ", w:"ʍ", x:"x", y:"ʎ", z:"z",
                A:"∀", B:"𐐒", C:"Ɔ", D:"◖", E:"Ǝ", F:"Ⅎ", G:"פ", H:"H", I:"I",
                J:"ſ", K:"ʞ", L:"˥", M:"W", N:"N", O:"O", P:"Ԁ", Q:"Ό", R:"ᴚ",
                S:"S", T:"┴", U:"∩", V:"Λ", W:"M", X:"X", Y:"⅄", Z:"Z",
                "?":"¿", "!":"¡"
            };
            return Array.from(text).map(c => flipMap[c] || c).reverse().join('');
        }
    };

    // Mirrored letters
    maps.mirror = {
        transform: text => {
            const mirrorMap = {
                a:"ɒ", b:"d", c:"ɔ", d:"b", e:"ɘ", f:"ʇ", g:"ϱ", h:"ʜ", i:"ι", j:"ſ",
                k:"ʞ", l:"|", m:"ɯ", n:"u", o:"o", p:"q", q:"p", r:"ɿ", s:"ƨ", t:"ƚ",
                u:"י", v:"ʌ", w:"ʍ", x:"x", y:"ʎ", z:"z",
                A:"∀", B:"𐐒", C:"Ɔ", D:"◖", E:"Ǝ", F:"Ⅎ", G:"פ", H:"H", I:"I",
                J:"ſ", K:"ʞ", L:"˥", M:"W", N:"И", O:"O", P:"Ԁ", Q:"Ό", R:"Я",
                S:"S", T:"⊥", U:"∩", V:"Λ", W:"M", X:"X", Y:"⅄", Z:"Z"
            };
            return Array.from(text).map(c => mirrorMap[c] || c).join('');
        }
    };

    // Runic
    maps.runic = {
        lower: toChars("ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛚᛗᛜᛟᛞᛝᛋᛏᚢᚹᛉᛪᛦᛇ"),
        upper: toChars("ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛚᛗᛜᛟᛞᛝᛋᛏᚢᚹᛉᛪᛦᛇ"),
        digits: baseDigits
    };

    // Hearts style
    maps.hearts = {
        transform: text => Array.from(text).map(c => `${c}❤`).join('')
    };

    // --------------------------------------------------

    function translate(text, mapKey) {
        if (!text) return "";
        const map = maps[mapKey];

        // styles with custom transform()
        if (map && map.transform) {
            return map.transform(text);
        }
        if (!map) return text;

        let result = "";

        for (let char of Array.from(text)) {
            const li = baseLower.indexOf(char);
            const ui = baseUpper.indexOf(char);
            const di = baseDigits.indexOf(char);

            if (li !== -1 && map.lower && map.lower[li]) {
                result += map.lower[li];
            } else if (ui !== -1 && map.upper && map.upper[ui]) {
                result += map.upper[ui];
            } else if (di !== -1 && map.digits && map.digits[di]) {
                result += map.digits[di];
            } else {
                result += char;
            }
        }

        return result;
    }


export { translate };
