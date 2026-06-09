/* static.c - manually updated */
#include <re_types.h>
#include <re_mod.h>

extern const struct mod_export exports_g711;
extern const struct mod_export exports_ice;
extern const struct mod_export exports_opensles;
extern const struct mod_export exports_opus;
extern const struct mod_export exports_opus_multistream;
extern const struct mod_export exports_stun;
extern const struct mod_export exports_turn;
extern const struct mod_export exports_uuid;


const struct mod_export *mod_table[] = {
	&exports_g711,
	&exports_ice,
	&exports_opensles,
	&exports_opus,
	&exports_opus_multistream,
	&exports_stun,
	&exports_turn,
	&exports_uuid,
	NULL
};
