package com.example.day.core.core_features.agent.di

import com.example.day.core.core_features.agent.domain.workers.branch.DeleteBranchCommandHandler
import com.example.day.core.core_features.agent.domain.workers.branch.ListBranchesCommandHandler
import com.example.day.core.core_features.agent.domain.workers.branch.NewBranchCommandHandler
import com.example.day.core.core_features.agent.domain.workers.branch.SwitchBranchCommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.CommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.ContextCommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.InfoCommandHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup.SetupBranchingHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup.SetupSlidingWindowHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup.SetupStickyFactsHandler
import com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup.SetupSummarizationHandler
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
abstract class CommandHandlerModule {

    @Binds
    @IntoSet
    abstract fun bindInfoHandler(handler: InfoCommandHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindContextHandler(handler: ContextCommandHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindSetupSummarizationHandler(handler: SetupSummarizationHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindSetupSlidingWindowHandler(handler: SetupSlidingWindowHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindSetupStickyFactsHandler(handler: SetupStickyFactsHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindSetupBranchingHandler(handler: SetupBranchingHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindNewBranchHandler(handler: NewBranchCommandHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindSwitchBranchHandler(handler: SwitchBranchCommandHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindListBranchesHandler(handler: ListBranchesCommandHandler): CommandHandler

    @Binds
    @IntoSet
    abstract fun bindDeleteBranchHandler(handler: DeleteBranchCommandHandler): CommandHandler
}
