/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.commands

/**
 * Contains a list of commands that can be sent to a sequencer
 *
 * @param commands sequence of SequenceCommand
 */
final case class CommandList(commands: Seq[SequenceCommand])
